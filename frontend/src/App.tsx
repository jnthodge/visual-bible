import { ChangeEvent, useEffect, useMemo, useState } from 'react';
import { BibleImageRecord, HighlightRegion } from './types/api';

const API_URL = 'http://localhost:8080/api/projects';

export function App() {
  const [records, setRecords] = useState<BibleImageRecord[]>([]);
  const [selectedId, setSelectedId] = useState<string>('');
  const [name, setName] = useState('');
  const [outputPath, setOutputPath] = useState('/workspace/visual-bible/data/output');
  const [passageFile, setPassageFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [hovered, setHovered] = useState<HighlightRegion | null>(null);

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
      setSelectedId(data[0].id);
    }
  }

  useEffect(() => {
    loadRecords();
  }, []);

  async function submitForm() {
    if (!passageFile || !name.trim() || !outputPath.trim()) {
      setMessage('Please provide a passage file, name, and output path.');
      return;
    }

    setLoading(true);
    setMessage('Generating image...');

    const form = new FormData();
    form.append('passagesFile', passageFile);
    form.append('name', name);
    form.append('outputPath', outputPath);

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
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <h2>Create Passage Map</h2>
        <label>
          Passage list file (.txt)
          <input type="file" accept=".txt" onChange={onFileChange} />
        </label>
        <label>
          Name
          <input value={name} onChange={(event) => setName(event.target.value)} placeholder="My favorite verses" />
        </label>
        <label>
          Save output path
          <input value={outputPath} onChange={(event) => setOutputPath(event.target.value)} />
        </label>
        <button onClick={submitForm} disabled={loading}>{loading ? 'Working...' : 'Generate + Save'}</button>
        <p className="message">{message}</p>

        <h3>Saved Images</h3>
        <ul>
          {records.map((record) => (
            <li key={record.id}>
              <button className={record.id === selectedId ? 'active' : ''} onClick={() => setSelectedId(record.id)}>
                {record.name}
              </button>
            </li>
          ))}
        </ul>
      </aside>

      <main className="viewer">
        {selectedRecord ? (
          <>
            <h1>{selectedRecord.name}</h1>
            <p>{selectedRecord.highlights.length} highlighted references detected.</p>
            <div className="image-wrapper" onMouseLeave={() => setHovered(null)}>
              <img src={`${API_URL}/${selectedRecord.id}/image`} alt={selectedRecord.name} />
              {selectedRecord.highlights.map((highlight) => (
                <div
                  key={`${highlight.verse}-${highlight.x}-${highlight.y}`}
                  className="hotspot"
                  style={{ left: highlight.x, top: highlight.y, width: highlight.width, height: highlight.height }}
                  onMouseEnter={() => setHovered(highlight)}
                />
              ))}
            </div>
          </>
        ) : (
          <p>No saved items yet.</p>
        )}
      </main>

      <aside className="detail-panel">
        <h3>Highlighted Verse</h3>
        {hovered ? (
          <>
            <strong>{hovered.verse}</strong>
            <p>{hovered.text}</p>
          </>
        ) : (
          <p>Hover over a red highlighted area to inspect verse text.</p>
        )}
      </aside>
    </div>
  );
}
