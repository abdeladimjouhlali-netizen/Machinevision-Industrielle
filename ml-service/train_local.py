"""Fine-tune the existing image classifier on the project's labelled photos.

Run from the project root with the ML virtual environment's Python:
    python ml-service/train_local.py

The original demo model is preserved; a new model and training report are saved.
"""

import json
import os
import random
from collections import Counter
from pathlib import Path

os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "2")

import tensorflow as tf

ROOT = Path(__file__).resolve().parents[1]
IMAGE_ROOT = ROOT / "data" / "images"
MODEL_DIR = ROOT / "ml-service" / "models"
CLASS_NAMES_PATH = ROOT / "ml-service" / "class_names.json"
SOURCE_MODEL = MODEL_DIR / "modele_demo_machines.keras"
OUTPUT_MODEL = MODEL_DIR / "modele_machines_local.keras"
REPORT_PATH = MODEL_DIR / "rapport_entrainement_local.json"
IMAGE_SIZE = (160, 160)
SEED = 42


def collect_data(class_names):
    """Keep one image per class for a tiny holdout; train on all remaining images."""
    rng = random.Random(SEED)
    train_paths, train_labels, val_paths, val_labels = [], [], [], []
    counts = {}
    for label, name in enumerate(class_names):
        paths = sorted(
            path
            for path in (IMAGE_ROOT / name).glob("*")
            if path.suffix.lower() in {".jpg", ".jpeg", ".png", ".bmp", ".webp"}
        )
        if len(paths) < 2:
            raise ValueError(f"Il faut au moins 2 images dans data/images/{name}.")
        rng.shuffle(paths)
        val_paths.append(str(paths[0]))
        val_labels.append(label)
        train_paths.extend(str(path) for path in paths[1:])
        train_labels.extend([label] * (len(paths) - 1))
        counts[name] = len(paths)
    return train_paths, train_labels, val_paths, val_labels, counts


def make_dataset(paths, labels, training):
    dataset = tf.data.Dataset.from_tensor_slices((paths, labels))

    def load_image(path, label):
        image = tf.io.read_file(path)
        image = tf.io.decode_image(image, channels=3, expand_animations=False)
        image.set_shape([None, None, 3])
        image = tf.image.resize(image, IMAGE_SIZE)
        return image, label

    dataset = dataset.map(load_image, num_parallel_calls=tf.data.AUTOTUNE)
    if training:
        dataset = dataset.shuffle(len(paths), seed=SEED, reshuffle_each_iteration=True)
    return dataset.batch(4).prefetch(tf.data.AUTOTUNE)


def main():
    if not SOURCE_MODEL.exists():
        raise FileNotFoundError(f"Modèle de départ absent : {SOURCE_MODEL}")
    class_names = json.loads(CLASS_NAMES_PATH.read_text(encoding="utf-8"))
    train_paths, train_labels, val_paths, val_labels, counts = collect_data(class_names)
    train_ds = make_dataset(train_paths, train_labels, training=True)
    val_ds = make_dataset(val_paths, val_labels, training=False)

    model = tf.keras.models.load_model(SOURCE_MODEL)
    model.trainable = True
    for layer in model.layers:
        if "mobilenetv2" in layer.name.lower():
            layer.trainable = False

    augmentation = tf.keras.Sequential(
        [
            tf.keras.layers.RandomFlip("horizontal", seed=SEED),
            tf.keras.layers.RandomRotation(0.04, fill_mode="nearest", seed=SEED),
            tf.keras.layers.RandomZoom(0.08, fill_mode="nearest", seed=SEED),
        ],
        name="augmentation_entrainement",
    )
    inputs = tf.keras.Input(shape=(*IMAGE_SIZE, 3), name="image")
    outputs = model(augmentation(inputs))
    trained_model = tf.keras.Model(inputs, outputs, name="reconnaissance_machines_locale")
    trained_model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=1e-4),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )

    class_counts = Counter(train_labels)
    class_weights = {
        index: len(train_labels) / (len(class_names) * class_counts[index])
        for index in range(len(class_names))
    }
    early_stopping = tf.keras.callbacks.EarlyStopping(
        monitor="val_loss", patience=6, restore_best_weights=True
    )
    history = trained_model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=30,
        callbacks=[early_stopping],
        class_weight=class_weights,
        verbose=2,
    )
    val_loss, val_accuracy = trained_model.evaluate(val_ds, verbose=0)
    trained_model.save(OUTPUT_MODEL)

    report = {
        "model": OUTPUT_MODEL.name,
        "source_model_preserved": SOURCE_MODEL.name,
        "classes": class_names,
        "images_per_class": counts,
        "train_images": len(train_paths),
        "validation_images": len(val_paths),
        "epochs_run": len(history.history["loss"]),
        "final_validation_loss": float(val_loss),
        "final_validation_accuracy": float(val_accuracy),
        "warning": (
            "Petit jeu de données de démonstration : une seule image par classe est réservée "
            "à la validation. Cette mesure ne prouve pas une bonne reconnaissance de nouvelles machines."
        ),
    }
    REPORT_PATH.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    print(f"\nModèle entraîné : {OUTPUT_MODEL}")
    print(f"Rapport : {REPORT_PATH}")


if __name__ == "__main__":
    main()
