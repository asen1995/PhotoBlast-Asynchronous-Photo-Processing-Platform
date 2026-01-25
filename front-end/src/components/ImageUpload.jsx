import { useState, useRef } from 'react';
import './ImageUpload.css';

const generateIdempotencyKey = () => {
  return `${Date.now()}-${Math.random().toString(36).substring(2, 11)}`;
};

const PROCESSING_TASKS = [
  { id: 'RESIZE', label: 'Resize', description: 'Resize to 1920x1080' },
  { id: 'THUMBNAIL', label: 'Thumbnail', description: 'Generate 200x200 thumbnail' },
  { id: 'WATERMARK', label: 'Watermark', description: 'Apply watermark overlay' },
];

function ImageUpload() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [selectedTasks, setSelectedTasks] = useState(['RESIZE', 'THUMBNAIL']);
  const [uploading, setUploading] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const [result, setResult] = useState(null);
  const [idempotencyKey, setIdempotencyKey] = useState(null);
  const fileInputRef = useRef(null);

  const handleFileSelect = (file) => {
    if (file && file.type.startsWith('image/')) {
      setSelectedFile(file);
      setPreview(URL.createObjectURL(file));
      setResult(null);
      setIdempotencyKey(generateIdempotencyKey());
    }
  };

  const handleInputChange = (e) => {
    const file = e.target.files?.[0];
    if (file) handleFileSelect(file);
  };

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    const file = e.dataTransfer.files?.[0];
    if (file) handleFileSelect(file);
  };

  const handleTaskToggle = (taskId) => {
    setSelectedTasks((prev) =>
      prev.includes(taskId)
        ? prev.filter((t) => t !== taskId)
        : [...prev, taskId]
    );
  };

  const handleUpload = async () => {
    if (!selectedFile || selectedTasks.length === 0) return;

    setUploading(true);
    setResult(null);

    const formData = new FormData();
    formData.append('file', selectedFile);

    const tasksParam = selectedTasks.join(',');

    try {
      const response = await fetch(`/api/photos/upload?tasks=${tasksParam}`, {
        method: 'POST',
        body: formData,
        headers: {
          'X-Idempotency-Key': idempotencyKey,
        },
      });

      const data = await response.json();

      if (response.ok) {
        setResult({ success: true, data });
        setSelectedFile(null);
        setPreview(null);
      } else {
        setResult({ success: false, message: data.message || 'Upload failed' });
      }
    } catch (error) {
      setResult({ success: false, message: 'Network error: ' + error.message });
    } finally {
      setUploading(false);
    }
  };

  const handleClear = () => {
    setSelectedFile(null);
    setPreview(null);
    setResult(null);
    setIdempotencyKey(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div className="upload-container">
      <h1>PhotoBlast</h1>
      <p className="subtitle">Upload and process your images</p>

      <div
        className={`drop-zone ${dragActive ? 'active' : ''} ${preview ? 'has-preview' : ''}`}
        onDragEnter={handleDrag}
        onDragLeave={handleDrag}
        onDragOver={handleDrag}
        onDrop={handleDrop}
        onClick={() => !preview && fileInputRef.current?.click()}
      >
        {preview ? (
          <div className="preview-container">
            <img src={preview} alt="Preview" className="preview-image" />
            <button className="clear-btn" onClick={handleClear}>
              Remove
            </button>
          </div>
        ) : (
          <div className="drop-zone-content">
            <div className="upload-icon">+</div>
            <p>Drag & drop an image here</p>
            <p className="or-text">or click to browse</p>
          </div>
        )}
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          onChange={handleInputChange}
          className="file-input"
        />
      </div>

      <div className="tasks-section">
        <h3>Processing Tasks</h3>
        <div className="tasks-grid">
          {PROCESSING_TASKS.map((task) => (
            <label key={task.id} className="task-option">
              <input
                type="checkbox"
                checked={selectedTasks.includes(task.id)}
                onChange={() => handleTaskToggle(task.id)}
              />
              <span className="task-label">{task.label}</span>
              <span className="task-description">{task.description}</span>
            </label>
          ))}
        </div>
      </div>

      <button
        className="upload-btn"
        onClick={handleUpload}
        disabled={!selectedFile || selectedTasks.length === 0 || uploading}
      >
        {uploading ? 'Uploading...' : 'Upload & Process'}
      </button>

      {result && (
        <div className={`result ${result.success ? 'success' : 'error'}`}>
          {result.success ? (
            <>
              <p>Upload successful!</p>
              <p className="result-detail">Job ID: {result.data.jobId}</p>
              <p className="result-detail">Photo ID: {result.data.photoId}</p>
            </>
          ) : (
            <p>{result.message}</p>
          )}
        </div>
      )}
    </div>
  );
}

export default ImageUpload;
