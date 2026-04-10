import shutil
from pathlib import Path

from app.services.types import SeOutput, VadSegmentType


class MockSeProvider:
    provider_name = "mock-se"

    def __init__(self, enhanced_dir: str):
        self._enhanced_dir = Path(enhanced_dir)

    def enhance(self, task_id: int, original_file_path: str, vad_segments: list[VadSegmentType] | None = None) -> SeOutput:
        _ = vad_segments

        src = Path(original_file_path)
        suffix = src.suffix if src.suffix else ".wav"
        stem = src.stem if src.stem else f"task_{task_id}"

        self._enhanced_dir.mkdir(parents=True, exist_ok=True)
        target = self._enhanced_dir / f"{stem}_enhanced{suffix}"

        if src.exists() and src.is_file():
            try:
                shutil.copyfile(src, target)
            except Exception:
                target = Path(f"/data/audio/enhanced/{stem}_enhanced{suffix}")
        else:
            fallback_parent = Path("/data/audio/enhanced")
            target = fallback_parent / f"{stem}_enhanced{suffix}"

        return SeOutput(enhanced_file_path=str(target))
