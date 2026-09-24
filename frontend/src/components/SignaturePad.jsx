import { useEffect, useRef, useState } from 'react';

/** Canvas signature capture that works with both mouse and touch, exported as a PNG data URL. */
export default function SignaturePad({ onChange, height = 180 }) {
  const canvasRef = useRef(null);
  const drawing = useRef(false);
  const [hasInk, setHasInk] = useState(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    const ratio = window.devicePixelRatio || 1;
    canvas.width = canvas.offsetWidth * ratio;
    canvas.height = height * ratio;
    const context = canvas.getContext('2d');
    context.scale(ratio, ratio);
    context.lineWidth = 2;
    context.lineCap = 'round';
    context.strokeStyle = '#0E2233';
  }, [height]);

  const positionOf = (event) => {
    const rect = canvasRef.current.getBoundingClientRect();
    const source = event.touches ? event.touches[0] : event;
    return { x: source.clientX - rect.left, y: source.clientY - rect.top };
  };

  const start = (event) => {
    event.preventDefault();
    drawing.current = true;
    const { x, y } = positionOf(event);
    const context = canvasRef.current.getContext('2d');
    context.beginPath();
    context.moveTo(x, y);
  };

  const move = (event) => {
    if (!drawing.current) return;
    event.preventDefault();
    const { x, y } = positionOf(event);
    const context = canvasRef.current.getContext('2d');
    context.lineTo(x, y);
    context.stroke();
    if (!hasInk) setHasInk(true);
  };

  const end = () => {
    if (!drawing.current) return;
    drawing.current = false;
    onChange(canvasRef.current.toDataURL('image/png'));
  };

  const clear = () => {
    const canvas = canvasRef.current;
    canvas.getContext('2d').clearRect(0, 0, canvas.width, canvas.height);
    setHasInk(false);
    onChange('');
  };

  return (
    <div>
      <div className="rounded-md border border-dashed border-edge bg-white">
        <canvas
          ref={canvasRef}
          style={{ height, width: '100%', touchAction: 'none' }}
          onMouseDown={start}
          onMouseMove={move}
          onMouseUp={end}
          onMouseLeave={end}
          onTouchStart={start}
          onTouchMove={move}
          onTouchEnd={end}
        />
      </div>
      <div className="mt-2 flex items-center justify-between text-xs text-harbour/55">
        <span>{hasInk ? 'Signature captured' : 'Ask the recipient to sign above'}</span>
        <button type="button" onClick={clear} className="font-medium text-brand hover:underline">
          Clear
        </button>
      </div>
    </div>
  );
}
