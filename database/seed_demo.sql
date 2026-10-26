USE machines;

-- Demonstration records only. Replace them with verified asset details and OEM PDFs.
INSERT INTO equipment (
    asset_tag,
    category,
    manufacturer,
    model_name,
    technical_sheet_filename,
    manual_filename
) VALUES
    (
        'DEMO-CONV-001',
        'convoyeur',
        'A identifier (DEMO)',
        'Convoyeur a rouleaux courbe - a confirmer',
        'fiches_techniques/DEMO-CONV-001.pdf',
        NULL
    ),
    (
        'DEMO-PMP-001',
        'pompe',
        'A identifier (DEMO)',
        'Pompe industrielle - type a confirmer',
        'fiches_techniques/DEMO-PMP-001.pdf',
        NULL
    ),
    (
        'DEMO-CMP-001',
        'compresseur',
        'A identifier (DEMO)',
        'Compresseur industriel - type a confirmer',
        'fiches_techniques/DEMO-CMP-001.pdf',
        NULL
    )
ON DUPLICATE KEY UPDATE
    category = VALUES(category),
    manufacturer = VALUES(manufacturer),
    model_name = VALUES(model_name),
    technical_sheet_filename = VALUES(technical_sheet_filename),
    manual_filename = NULL;
