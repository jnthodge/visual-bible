import { ChangeEvent, useEffect, useMemo, useState } from 'react';
import { BibleImageRecord, HighlightRegion } from './types/api';

const API_URL = 'http://localhost:8080/api/projects';

export function App() {
  const [records, setRecords] = useState<BibleImageRecord[]>([]);
  const [selectedId, setSelectedId] = useState<string>('');
  const [name, setName] = useState('');
  const [outputPath, setOutputPath] = useState('/workspace/visual-bible/data/output');
  const [typedReferences, setTypedReferences] = useState('Romans 11\nJeremiah 30:3-34:4\nAc1:12\n1Jn2:3');
  const [passageFile, setPassageFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [selectedHighlight, setSelectedHighlight] = useState<HighlightRegion | null>(null);
  const [zoom, setZoom] = useState(0.22);

  const selectedRecord = useMemo(
    () => records.find((item) => item.id === selectedId) ?? null,
    [records, selectedId]
  );

  async function loadRecords() {
    const response = await fetch(API_URL);
    if (!response.ok) return;
    const data: BibleImageRecord[] = await response.json();
    setRecords(data);
    if (!selectedId && data.length > 0) {
      setSelectedId(data[data.length - 1].id);
    }
  }

  useEffect(() => {
    loadRecords();
  }, []);

  async function submitForm() {
    if (!name.trim() || !outputPath.trim()) {
      setMessage('Please provide a name and output path.');
      return;
    }

    setLoading(true);
    setMessage('Generating image...');

    const form = new FormData();
    if (passageFile) {
      form.append('passagesFile', passageFile);
    }
    form.append('name', name);
    form.append('outputPath', outputPath);
    form.append('textReferences', typedReferences);

    const response = await fetch(API_URL, { method: 'POST', body: form });
    if (!response.ok) {
      setMessage('Failed to generate image.');
      setLoading(false);
      return;
    }

    setMessage('Saved successfully.');
    await loadRecords();
    setLoading(false);
  }

  function onFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;
    setPassageFile(file);
    if (!file) {
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const text = typeof reader.result === 'string' ? reader.result : '';
      setTypedReferences(text);
      setMessage(`Loaded ${file.name} into editable memo field.`);
    };
    reader.readAsText(file);
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <h2>Create Topic View</h2>
        <label>
          Passage list file (.txt)
          <input type="file" accept=".txt" onChange={onFileChange} />
        </label>
        <label>
          Memo field (editable references)
          <textarea value={typedReferences} onChange={(event) => setTypedReferences(event.target.value)} rows={9} />
        </label>
        <label>
          Topic Name
          <input value={name} onChange={(event) => setName(event.target.value)} placeholder="Rapture, Baptism, Tithing..." />
        </label>
        <label>
          Save output path
          <input value={outputPath} onChange={(event) => setOutputPath(event.target.value)} />
        </label>
        <button onClick={submitForm} disabled={loading}>{loading ? 'Working...' : 'Generate + Save'}</button>
        <p className="message">{message}</p>

        <h3>Saved Topic / Passage Maps</h3>
        <div className="list-view">
          <div className="list-header"><span>Name</span><span>Refs</span><span>Created</span></div>
          {records.map((record) => (
            <button
              key={record.id}
              className={`row-btn ${record.id === selectedId ? 'active' : ''}`}
              onClick={() => {
                setSelectedId(record.id);
                setSelectedHighlight(null);
              }}
            >
              <span>{record.name}</span>
              <span>{record.references.length}</span>
              <span>{new Date(record.createdAt).toLocaleDateString()}</span>
            </button>
          ))}
        </div>
      </aside>

      <main className="viewer">
        {selectedRecord ? (
          <>
            <h1>{selectedRecord.name}</h1>
            <p>{selectedRecord.highlights.length} highlighted references mapped in bright red.</p>
            <p className="meta">Image: {selectedRecord.imagePath}</p>
            <div className="zoom-row">
              <label htmlFor="zoom">Zoom</label>
              <input id="zoom" type="range" min={0.05} max={4} step={0.01} value={zoom} onChange={(e) => setZoom(Number(e.target.value))} />
              <span>{Math.round(zoom * 100)}%</span>
            </div>
            <div className="image-viewport">
              <div className="image-wrapper" style={{ transform: `scale(${zoom})`, transformOrigin: 'top left' }}>
                <img src={`${API_URL}/${selectedRecord.id}/image`} alt={selectedRecord.name} />
                {selectedRecord.highlights.map((highlight) => (
                  <button
                    key={`${highlight.verse}-${highlight.x}-${highlight.y}`}
                    className={`hotspot ${selectedHighlight?.verse === highlight.verse ? 'selected' : ''}`}
                    style={{ left: highlight.x, top: highlight.y, width: highlight.width, height: highlight.height }}
                    onClick={() => setSelectedHighlight(highlight)}
                    title={highlight.verse}
                  />
                ))}
              </div>
            </div>
          </>
        ) : (
          <p>No saved items yet.</p>
        )}
      </main>

      <aside className="detail-panel">
        <h3>Selected Highlight</h3>
        {selectedHighlight ? (
          <>
            <strong>{selectedHighlight.verse}</strong>
            <p>{selectedHighlight.text}</p>
          </>
        ) : (
          <p>Click on bright-red areas in the image to inspect verse text.</p>
        )}

        {selectedRecord && (
          <>
            <h4>Passage List ({selectedRecord.references.length})</h4>
            <div className="refs-list">
              {selectedRecord.references.map((r) => <div key={r}>{r}</div>)}
            </div>
          </>
        )}
      </aside>
    </div>
  );
}
