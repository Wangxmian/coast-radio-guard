import logging
from pathlib import Path

from app.services.exceptions import ProviderUnavailableError

logger = logging.getLogger(__name__)



def resolve_model_source(stage: str, model_name: str, model_dir: str) -> str:
    if model_dir:
        path = Path(model_dir).expanduser()
        if not path.exists():
            raise ProviderUnavailableError(f"{stage} local model dir not found: {path}")
        logger.info("%s model source: local_dir=%s", stage, path)
        return str(path)

    if not model_name:
        raise ProviderUnavailableError(f"{stage} model name is empty")

    logger.info("%s model source: hub_model=%s", stage, model_name)
    return model_name



def snapshot_download_model(model_id: str, local_dir: str | None = None) -> str:
    """Optional helper for manual pre-download (not auto-called in request path)."""
    try:
        from modelscope import snapshot_download  # type: ignore
    except Exception as ex:  # pragma: no cover
        raise ProviderUnavailableError(f"modelscope is required for snapshot download: {ex}") from ex

    kwargs = {"model_id": model_id}
    if local_dir:
        kwargs["local_dir"] = local_dir

    model_dir = snapshot_download(**kwargs)
    logger.info("modelscope snapshot downloaded: model=%s dir=%s", model_id, model_dir)
    return str(model_dir)
