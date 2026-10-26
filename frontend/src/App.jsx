import { useState } from "react";

const categoryLabels = {
  pompe: "Pompe",
  compresseur: "Compresseur",
  convoyeur: "Convoyeur",
};

function App() {
  const [file, setFile] = useState(null);
  const [assetTag, setAssetTag] = useState("");
  const [preview, setPreview] = useState("");
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  function handleImage(event) {
    const selected = event.target.files?.[0] ?? null;
    setFile(selected);
    setResult(null);
    setError("");
    setPreview(selected ? URL.createObjectURL(selected) : "");
  }

  async function analyze(event) {
    event.preventDefault();
    if (!file) {
      setError("Choisis ou prends une photo avant de lancer l’analyse.");
      return;
    }

    setLoading(true);
    setError("");
    setResult(null);
    const form = new FormData();
    form.append("file", file);
    if (assetTag.trim()) form.append("assetTag", assetTag.trim());

    try {
      const response = await fetch("/api/predictions", { method: "POST", body: form });
      const responseText = await response.text();
      let data = {};
      if (responseText) {
        try {
          data = JSON.parse(responseText);
        } catch {
          throw new Error(`L’API a renvoyé une réponse illisible (HTTP ${response.status}).`);
        }
      }
      if (!response.ok) throw new Error(data.detail || data.message || "L’analyse a échoué.");
      if (!responseText) throw new Error("L’API n’a renvoyé aucun résultat.");

      let equipmentMatches = [];
      let equipmentLookupError = "";
      try {
        const lookup = await fetch(`/api/equipment?category=${encodeURIComponent(data.category)}`);
        if (!lookup.ok) throw new Error("Réponse indisponible");
        equipmentMatches = await lookup.json();
      } catch {
        equipmentLookupError = "Impossible de charger les équipements enregistrés pour cette catégorie.";
      }
      setResult({ ...data, equipmentMatches, equipmentLookupError });
    } catch (err) {
      setError(`${err.message} Vérifie que les services de l’application sont démarrés.`);
    } finally {
      setLoading(false);
    }
  }

  const label = categoryLabels[result?.category] ?? result?.category ?? "";
  const confidence = Math.max(0, Math.min(100, (result?.confidence ?? 0) * 100));
  const equipment = result?.equipment;
  const assetCategoryMatches = equipment?.category?.toLowerCase() === result?.category?.toLowerCase();
  const records = [...(result?.equipmentMatches ?? [])];
  if (assetCategoryMatches && !records.some((item) => item.assetTag === equipment.assetTag)) {
    records.unshift(equipment);
  }
  if (assetTag.trim()) {
    records.sort((left, right) => Number(right.assetTag === assetTag.trim()) - Number(left.assetTag === assetTag.trim()));
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <a className="brand" href="#top" aria-label="MachineVision accueil">
          <span className="brand-mark" aria-hidden="true">M</span>
          <span>Machine<span className="brand-light">Vision</span></span>
        </a>
        <span className="system-status"><i /> Assistant de maintenance</span>
      </header>

      <section className="hero" id="top">
        <div className="hero-copy">
          <p className="eyebrow">RECONNAISSANCE DES ÉQUIPEMENTS</p>
          <h1>La bonne machine.<br /><em>La bonne documentation.</em></h1>
          <p className="hero-description">
            Prends une photo pour identifier le type de machine, puis retrouve les équipements
            enregistrés et leurs documents disponibles.
          </p>
          <div className="workflow" aria-label="Étapes">
            <span><b>1</b> Photo</span><span className="workflow-line" />
            <span><b>2</b> Identification</span><span className="workflow-line" />
            <span><b>3</b> Fiche technique</span>
          </div>
        </div>
        <div className="hero-orbit" aria-hidden="true"><span>MV</span></div>
      </section>

      <form className="workspace" onSubmit={analyze}>
        <section className="panel capture-panel">
          <div className="section-heading">
            <span className="step-number">01</span>
            <div><h2>Photographier l’équipement</h2><p>Utilise une photo nette, où la machine est bien visible.</p></div>
          </div>

          <label className={`upload-zone ${preview ? "has-preview" : ""}`}>
            {preview ? (
              <img className="preview-image" src={preview} alt="Aperçu de la machine sélectionnée" />
            ) : (
              <span className="upload-placeholder">
                <span className="camera-icon" aria-hidden="true">＋</span>
                <strong>Prendre une photo ou choisir un fichier</strong>
                <small>Formats acceptés : JPG, PNG, WEBP</small>
              </span>
            )}
            <input type="file" accept="image/*" capture="environment" onChange={handleImage} />
          </label>

          <label className="field-label" htmlFor="asset-tag">Numéro d’actif <span>— si l’équipement a une étiquette</span></label>
          <input
            id="asset-tag"
            className="text-input"
            value={assetTag}
            onChange={(event) => setAssetTag(event.target.value)}
            placeholder="Ex. PMP-001"
            autoComplete="off"
          />
          <p className="field-hint">Le numéro d’actif aide à retrouver la fiche du modèle exact.</p>
          <button className="analyze-button" type="submit" disabled={loading}>
            {loading ? <><span className="spinner" aria-hidden="true" /> Analyse en cours…</> : <>Analyser la photo <span aria-hidden="true">→</span></>}
          </button>
          {error && <div className="error-message" role="alert">{error}</div>}
        </section>

        <section className="panel result-panel" aria-live="polite">
          <div className="section-heading">
            <span className="step-number">02</span>
            <div><h2>Identification et fiches</h2><p>Le résultat et les documents associés apparaissent ici.</p></div>
          </div>

          {!result ? (
            <div className="empty-state">
              <span className="empty-icon" aria-hidden="true">⌕</span>
              <strong>Prêt pour l’identification</strong>
              <p>Ajoute une photo pour rechercher les équipements correspondants.</p>
            </div>
          ) : (
            <div className="result-content">
              <div className="prediction-card">
                <div className="prediction-topline"><span className="result-kicker">TYPE RECONNU</span><span className="result-check">✓</span></div>
                <h3>{label}</h3>
                <div className="confidence-row"><span>Confiance du modèle</span><strong>{confidence.toFixed(1)}%</strong></div>
                <div className="meter" role="meter" aria-label="Confiance du modèle" aria-valuemin="0" aria-valuemax="100" aria-valuenow={confidence}>
                  <span style={{ width: `${confidence}%` }} />
                </div>
              </div>

              <div className="documents-section">
                <div className="documents-heading">
                  <div><span className="result-kicker">PARC ENREGISTRÉ</span><h3>Équipements correspondants</h3></div>
                  <span className="match-count">{records.length}</span>
                </div>

                {result.equipmentLookupError ? (
                  <div className="notice-card"><strong>Fiches indisponibles</strong><p>{result.equipmentLookupError}</p></div>
                ) : records.length ? (
                  <div className="equipment-list">
                    {records.map((item) => (
                      <article className="equipment-card" key={item.assetTag}>
                        <div className="equipment-heading">
                          <div><span className="asset-label">N° D’ACTIF</span><h4>{item.assetTag}</h4></div>
                          {item.assetTag === assetTag.trim() && <span className="asset-match">Actif saisi</span>}
                          {item.assetTag?.startsWith("DEMO-") && <span className="demo-badge">Exemple provisoire</span>}
                        </div>
                        <dl>
                          <div><dt>Fabricant</dt><dd>{item.manufacturer || "Non renseigné"}</dd></div>
                          <div><dt>Modèle</dt><dd>{item.model || "Non renseigné"}</dd></div>
                        </dl>
                        <div className="document-links">
                          {item.technicalSheetUrl
                            ? <a href={item.technicalSheetUrl} target="_blank" rel="noreferrer"><span aria-hidden="true">▤</span> Fiche d’identification PDF</a>
                            : <span className="document-missing">Fiche PDF à ajouter</span>}
                          {item.manualUrl
                            ? <a href={item.manualUrl} target="_blank" rel="noreferrer"><span aria-hidden="true">▤</span> Manuel officiel PDF</a>
                            : <span className="document-missing">Manuel officiel à ajouter</span>}
                        </div>
                      </article>
                    ))}
                  </div>
                ) : (
                  <div className="notice-card">
                    <span className="notice-icon" aria-hidden="true">i</span>
                    <div><strong>Aucune fiche liée à cette catégorie</strong>
                      <p>Le type de machine est reconnu, mais aucun équipement avec ses documents n’est encore enregistré. Demande à l’administrateur du parc d’ajouter la fiche correspondant à la marque et au modèle.</p>
                    </div>
                  </div>
                )}
              </div>
              <p className="safety-note"><strong>À vérifier :</strong> la photo donne une estimation du type, pas une identification garantie du modèle. Confirme toujours le numéro d’actif et la plaque signalétique.</p>
            </div>
          )}
        </section>
      </form>

      <footer><span className="footer-mark">M</span> MachineVision <span>·</span> Outil d’aide à l’identification, à confirmer sur l’équipement.</footer>
    </main>
  );
}

export default App;
