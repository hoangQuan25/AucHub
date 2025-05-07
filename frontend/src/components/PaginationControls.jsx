import React from 'react';

function PaginationControls({ pagination, onPageChange, isLoading }) {
  if (pagination.totalPages <= 1) return null;
  return (
    <div className="flex justify-center items-center gap-4 my-6">
      <button
        className="px-4 py-2 bg-white border rounded disabled:opacity-50"
        disabled={pagination.page === 0 || isLoading}
        onClick={() => onPageChange(pagination.page - 1)}
      >
        Previous
      </button>
      <span className="text-sm text-gray-600">
        Page {pagination.page + 1} of {Math.max(pagination.totalPages, 1)}
      </span>
      <button
        className="px-4 py-2 bg-white border rounded disabled:opacity-50"
        disabled={pagination.page >= pagination.totalPages - 1 || isLoading}
        onClick={() => onPageChange(pagination.page + 1)}
      >
        Next
      </button>
    </div>
  );
}

export default PaginationControls;