import json
import os
from contextlib import asynccontextmanager
from io import BytesIO
from pathlib import Path

import numpy as np
import tensorflow as tf
from fastapi import FastAPI, File, HTTPException, UploadFile
from PIL import Image, UnidentifiedImageError

MODEL_PATH = Path(os.getenv("MODEL_PATH", "models/modele_machines_local.keras"))
CLASS_NAMES_PATH = Path(os.getenv("CLASS_NAMES_PATH", "class_names.json"))
MAX_IMAGE_BYTES = 10 * 1024 * 1024


@asynccontextmanager
async def lifespan(app: FastAPI):
    try:
        app.state.model = tf.keras.models.load_model(MODEL_PATH)
        app.state.class_names = json.loads(CLASS_NAMES_PATH.read_text(encoding="utf-8"))
        if app.state.model.output_shape[-1] != len(app.state.class_names):
            raise ValueError("Le nombre de catégories ne correspond pas à la sortie du modèle.")
        app.state.model_error = None
    except Exception as exc:
        app.state.model = None
        app.state.class_names = []
        app.state.model_error = str(exc)
    yield


app = FastAPI(title="Industrial Machine Classifier", version="0.1.0", lifespan=lifespan)


@app.get("/health")
def health():
    if app.state.model is None:
        raise HTTPException(status_code=503, detail="Modèle absent ou impossible à charger.")
    return {"status": "ready", "categories": app.state.class_names}


@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(status_code=415, detail="Le fichier doit être une image.")
    if app.state.model is None:
        raise HTTPException(status_code=503, detail="Le modèle est indisponible.")

    content = await file.read(MAX_IMAGE_BYTES + 1)
    if len(content) > MAX_IMAGE_BYTES:
        raise HTTPException(status_code=413, detail="Image trop volumineuse (maximum 10 Mo).")

    try:
        image = Image.open(BytesIO(content)).convert("RGB").resize((160, 160))
    except (UnidentifiedImageError, OSError):
        raise HTTPException(status_code=400, detail="Image illisible ou format non pris en charge.")

    batch = np.expand_dims(np.asarray(image, dtype=np.float32), axis=0)
    scores = app.state.model.predict(batch, verbose=0)[0]
    best_index = int(np.argmax(scores))
    return {
        "category": app.state.class_names[best_index],
        "confidence": float(scores[best_index]),
        "scores": {
            name: float(scores[index])
            for index, name in enumerate(app.state.class_names)
        },
        "disclaimer": "Résultat expérimental : vérifiez l'identité de la machine sur sa plaque.",
    }
