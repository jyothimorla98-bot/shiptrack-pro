export default function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-between border-t border-edge px-4 py-3 text-sm">
      <span className="text-harbour/55">
        Page {page + 1} of {totalPages}
      </span>
      <div className="flex gap-2">
        <button
          type="button"
          className="btn-ghost px-3 py-1.5"
          disabled={page === 0}
          onClick={() => onChange(page - 1)}
        >
          Previous
        </button>
        <button
          type="button"
          className="btn-ghost px-3 py-1.5"
          disabled={page + 1 >= totalPages}
          onClick={() => onChange(page + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}
