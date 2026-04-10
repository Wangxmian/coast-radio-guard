import importlib.util
import logging
import sys
from datetime import datetime
from pathlib import Path
from typing import Any

from app.services.exceptions import ProviderUnavailableError
from app.services.types import SeOutput, VadSegmentType

logger = logging.getLogger(__name__)


class GtcrnSeProvider:
    provider_name = "gtcrn-se"

    def __init__(
        self,
        model_path: str,
        project_dir: str,
        checkpoint_dir: str,
        checkpoint_file: str,
        device: str,
        output_dir: str,
    ):
        self._device_name = device or "cpu"
        self._device = None
        self._output_dir = Path(output_dir or "./data/enhanced").expanduser()
        self._project_dir = Path(project_dir).expanduser() if project_dir else None
        self._checkpoint_dir = Path(checkpoint_dir).expanduser() if checkpoint_dir else None
        self._checkpoint_file = checkpoint_file.strip() if checkpoint_file else ""
        self._model_path = Path(model_path).expanduser() if model_path else None

        try:
            import numpy as np  # type: ignore
            import soundfile as sf  # type: ignore
            import torch  # type: ignore
        except Exception as ex:  # pragma: no cover
            raise ProviderUnavailableError(f"GTCRN dependencies missing: {ex}") from ex

        self._np = np
        self._sf = sf
        self._torch = torch
        self._resampler = _build_resampler()

        self._model = self._load_model()
        self._window = self._torch.hann_window(512).pow(0.5)

        logger.info(
            "GTCRN provider loaded: project_dir=%s checkpoint=%s device=%s output_dir=%s",
            self._project_dir,
            getattr(self, "_checkpoint_path", None),
            self._device_name,
            self._output_dir,
        )

    def enhance(self, task_id: int, original_file_path: str, vad_segments: list[VadSegmentType] | None = None) -> SeOutput:
        _ = vad_segments
        src = Path(original_file_path)
        if not src.exists():
            raise RuntimeError(f"GTCRN input audio not found: {original_file_path}")

        wav, sr = self._sf.read(str(src), dtype="float32")
        wav = _to_mono(wav, self._np)
        if sr != 16000:
            logger.info("GTCRN resampling input from %sHz to 16000Hz", sr)
            wav = self._resampler(wav, int(sr), 16000)

        tensor = self._torch.from_numpy(wav).float().to(self._device)
        window = self._window.to(self._device)

        stft = self._torch.stft(tensor, 512, 256, 512, window, return_complex=False)
        with self._torch.no_grad():
            out = self._model(stft[None])[0]

        if out.shape[-1] == 2:
            out_complex = self._torch.view_as_complex(out.contiguous())
        else:
            out_complex = out
        enh = self._torch.istft(out_complex, 512, 256, 512, window)
        enh_np = enh.detach().cpu().numpy().astype(self._np.float32)
        enh_np = self._np.clip(enh_np, -1.0, 1.0)

        self._output_dir.mkdir(parents=True, exist_ok=True)
        suffix = src.suffix if src.suffix else ".wav"
        ts = datetime.utcnow().strftime("%Y%m%d%H%M%S")
        out_path = self._output_dir / f"task{task_id}_{src.stem}_gtcrn_{ts}{suffix}"
        self._sf.write(str(out_path), enh_np, 16000)

        logger.info("GTCRN enhance success: taskId=%s input=%s output=%s", task_id, src, out_path)
        return SeOutput(enhanced_file_path=str(out_path))

    def _load_model(self):
        # Backward-compatible path for existing TorchScript deployments.
        if self._model_path and self._model_path.exists() and self._model_path.suffix == ".pt":
            self._device = self._torch.device(self._device_name)
            model = self._torch.jit.load(str(self._model_path), map_location=self._device)
            model.eval()
            self._checkpoint_path = self._model_path
            logger.info("GTCRN loaded as TorchScript model from %s", self._model_path)
            return model

        if not self._project_dir or not self._project_dir.exists():
            raise ProviderUnavailableError(
                "GTCRN project directory not found. Set GTCRN_PROJECT_DIR (e.g. /Users/wangxinmian/Downloads/project/gtcrn-main)."
            )

        if not self._checkpoint_dir:
            self._checkpoint_dir = self._project_dir / "checkpoints"
        if not self._checkpoint_dir.exists():
            raise ProviderUnavailableError(f"GTCRN checkpoint directory not found: {self._checkpoint_dir}")

        ckpt_path = self._resolve_checkpoint_path()
        gtcrn_class = self._import_gtcrn_class()

        self._device = self._torch.device(self._device_name)
        model = gtcrn_class().to(self._device).eval()
        ckpt = self._torch.load(str(ckpt_path), map_location=self._device)
        state = ckpt.get("model") if isinstance(ckpt, dict) and "model" in ckpt else ckpt
        model.load_state_dict(state)

        self._checkpoint_path = ckpt_path
        return model

    def _resolve_checkpoint_path(self) -> Path:
        if self._checkpoint_file:
            p = self._checkpoint_dir / self._checkpoint_file
            if not p.exists():
                raise ProviderUnavailableError(f"GTCRN checkpoint file not found: {p}")
            return p

        candidates = [
            self._checkpoint_dir / "model_trained_on_dns3.tar",
            self._checkpoint_dir / "model_trained_on_vctk.tar",
        ]
        for c in candidates:
            if c.exists():
                return c

        tar_files = sorted(self._checkpoint_dir.glob("*.tar"))
        if tar_files:
            return tar_files[0]

        raise ProviderUnavailableError(f"No GTCRN checkpoint .tar found in {self._checkpoint_dir}")

    def _import_gtcrn_class(self):
        module_path = self._project_dir / "gtcrn.py"
        if not module_path.exists():
            raise ProviderUnavailableError(f"GTCRN module not found: {module_path}")

        if str(self._project_dir) not in sys.path:
            sys.path.insert(0, str(self._project_dir))

        spec = importlib.util.spec_from_file_location("gtcrn_local", str(module_path))
        if spec is None or spec.loader is None:
            raise ProviderUnavailableError(f"Unable to import GTCRN module from {module_path}")

        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)  # type: ignore[union-attr]
        if not hasattr(module, "GTCRN"):
            raise ProviderUnavailableError(f"GTCRN class not found in {module_path}")
        return getattr(module, "GTCRN")


def _to_mono(audio: Any, np_mod) -> Any:
    if getattr(audio, "ndim", 1) == 1:
        return audio
    return audio.mean(axis=1).astype(np_mod.float32)


def _build_resampler():
    try:
        import torchaudio  # type: ignore
        import torch  # type: ignore

        def _resample_ta(audio: Any, source_sr: int, target_sr: int) -> Any:
            tensor = torch.from_numpy(audio).float().unsqueeze(0)
            out = torchaudio.functional.resample(tensor, source_sr, target_sr)
            return out.squeeze(0).numpy().astype("float32")

        return _resample_ta
    except Exception:
        logger.warning("GTCRN: torchaudio not available, fallback to numpy interpolation")

    def _resample_np(audio: Any, source_sr: int, target_sr: int) -> Any:
        import numpy as np

        if source_sr <= 0 or target_sr <= 0 or source_sr == target_sr:
            return audio
        duration = audio.shape[0] / float(source_sr)
        target_len = max(1, int(duration * target_sr))
        old_x = np.linspace(0.0, 1.0, num=audio.shape[0], endpoint=False)
        new_x = np.linspace(0.0, 1.0, num=target_len, endpoint=False)
        return np.interp(new_x, old_x, audio).astype(np.float32)

    return _resample_np
