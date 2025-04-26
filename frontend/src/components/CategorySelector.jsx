// src/components/CategorySelector.jsx (New File)
import React from 'react';

/**
 * Represents a single category item in the selector.
 */
const CategoryItem = ({ category, allCategories, selectedIds, onSelectionChange, level }) => {
  const isSelected = selectedIds.has(category.id);
  const children = allCategories.filter(cat => cat.parentId === category.id);

  const handleCheckboxChange = () => {
    // Tell the parent component to update the selection state
    onSelectionChange(category.id);
  };

  return (
    <li className={`ml-${level * 4} my-1`}> {/* Indentation based on level */}
      <label className="flex items-center space-x-2 cursor-pointer hover:bg-gray-100 p-1 rounded">
        <input
          type="checkbox"
          className="form-checkbox h-4 w-4 text-indigo-600 rounded focus:ring-indigo-500 border-gray-300"
          checked={isSelected}
          onChange={handleCheckboxChange}
        />
        <span className={`text-sm ${isSelected ? 'font-semibold' : ''}`}>{category.name}</span>
      </label>
      {/* Recursively render children if any */}
      {children.length > 0 && (
        <ul className="mt-1">
          {children.map(child => (
            <CategoryItem
              key={child.id}
              category={child}
              allCategories={allCategories}
              selectedIds={selectedIds}
              onSelectionChange={onSelectionChange}
              level={level + 1} // Increase indentation level
            />
          ))}
        </ul>
      )}
    </li>
  );
};

/**
 * Main Category Selector Component
 */
function CategorySelector({ categories = [], selectedIds = new Set(), onSelectionChange, isLoading, error }) {

  const handleCategoryToggle = (categoryId) => {
      const newIds = new Set(selectedIds); // Create mutable copy
      if (newIds.has(categoryId)) {
          newIds.delete(categoryId); // Toggle off
      } else {
          newIds.add(categoryId); // Toggle on
      }
      onSelectionChange(newIds); // Notify parent with the new Set
  };

  // Find top-level categories (no parentId)
  const topLevelCategories = categories.filter(cat => cat.parentId === null);

  if (isLoading) {
    return <p className="text-sm text-gray-500">Loading categories...</p>;
  }

  if (error) {
    return <p className="text-red-500 text-sm">{error}</p>;
  }

  if (!isLoading && categories.length === 0) {
      return <p className="text-sm text-gray-500">No categories found.</p>;
  }

  return (
    <div className="p-2 border rounded border-gray-300 max-h-60 overflow-y-auto bg-white">
       {topLevelCategories.length > 0 ? (
            <ul>
                {topLevelCategories.map(category => (
                    <CategoryItem
                        key={category.id}
                        category={category}
                        allCategories={categories} // Pass full list for recursion
                        selectedIds={selectedIds}
                        onSelectionChange={handleCategoryToggle} // Pass the toggle handler
                        level={0} // Start at level 0
                    />
                ))}
            </ul>
        ) : (
             <p className="text-sm text-gray-500">No top-level categories available.</p>
        )}

    </div>
  );
}

export default CategorySelector;