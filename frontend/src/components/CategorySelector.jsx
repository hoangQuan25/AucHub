import React, { useState } from 'react';
import { FaChevronRight, FaChevronDown } from 'react-icons/fa';

const CategoryItem = ({ category, allCategories, selectedIds, onSelectionChange }) => {
  const [collapsed, setCollapsed] = useState(true);
  const isSelected = selectedIds.has(category.id);
  const children = allCategories.filter(cat => cat.parentId === category.id);

  const toggleCollapse = (e) => {
    e.stopPropagation();
    setCollapsed(!collapsed);
  };

  // Notify parent to toggle this category ID
  const handleCheckboxChange = (e) => {
    e.stopPropagation();
    onSelectionChange(category.id);
  };

  return (
    <li className="group">
      <div className="flex items-center p-2 rounded-lg hover:bg-gray-100">
        {children.length > 0 && (
          <button
            onClick={toggleCollapse}
            className="flex-shrink-0 mr-2 text-gray-500 hover:text-gray-700 focus:outline-none"
            aria-label={collapsed ? 'Expand' : 'Collapse'}
          >
            {collapsed ? <FaChevronRight size={12} /> : <FaChevronDown size={12} />}
          </button>
        )}
        <label
          className="flex items-center space-x-2 cursor-pointer flex-1"
          onClick={handleCheckboxChange}
        >
          <input
            type="checkbox"
            className="form-checkbox h-4 w-4 text-indigo-600 rounded focus:ring-indigo-500 border-gray-300"
            checked={isSelected}
            onChange={() => {}}
          />
          <span className={isSelected ? 'font-semibold text-gray-900' : 'text-gray-700'}>
            {category.name}
          </span>
        </label>
      </div>
      {children.length > 0 && !collapsed && (
        <ul className="pl-4 border-l border-gray-200">
          {children.map(child => (
            <CategoryItem
              key={child.id}
              category={child}
              allCategories={allCategories}
              selectedIds={selectedIds}
              onSelectionChange={onSelectionChange}
            />
          ))}
        </ul>
      )}
    </li>
  );
};

function CategorySelector({ categories = [], selectedIds = new Set(), onSelectionChange, isLoading, error }) {
  if (isLoading) return <p className="text-sm text-gray-500 p-2">Loading categories...</p>;
  if (error) return <p className="text-red-500 text-sm p-2">{error}</p>;
  if (!isLoading && categories.length === 0) return <p className="text-sm text-gray-500 p-2">No categories found.</p>;

  // Wrap the parent onSelectionChange so we can toggle ID in a new Set
  const toggleCategory = (categoryId) => {
    const newSet = new Set(selectedIds);
    if (newSet.has(categoryId)) newSet.delete(categoryId);
    else newSet.add(categoryId);
    onSelectionChange(newSet);
  };

  const topLevel = categories.filter(cat => cat.parentId === null);

  return (
    <div className="border rounded border-gray-300 bg-white p-2 text-sm max-h-screen overflow-y-auto">
      <h3 className="px-2 py-1 font-medium text-gray-700 border-b mb-2">Filter by Category</h3>
      <ul className="space-y-1">
        {topLevel.map(category => (
          <CategoryItem
            key={category.id}
            category={category}
            allCategories={categories}
            selectedIds={selectedIds}
            onSelectionChange={toggleCategory}
          />
        ))}
      </ul>
    </div>
  );
}

export default CategorySelector;
// Note: This component is designed to be used in a modal or sidebar, so it has a max height and scrolls if necessary.