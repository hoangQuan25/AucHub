// src/components/CategorySelector.jsx (Ensure this version is used)
import React from 'react';

const CategoryItem = ({ category, allCategories, selectedIds, onSelectionChange, level }) => {
  const isSelected = selectedIds.has(category.id);
  // Find children by matching parentId
  const children = allCategories.filter(cat => cat.parentId === category.id);

  const handleCheckboxChange = () => {
    onSelectionChange(category.id); // Notify parent about the toggle
  };

  // Use Tailwind margin left based on level for indentation
  const indentationClass = `ml-${level * 4}`; // e.g., ml-0, ml-4, ml-8

  return (
    <li className={`${indentationClass} my-1`}>
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
        <ul className="mt-1"> {/* Child list */}
          {children.map(child => (
            <CategoryItem
              key={child.id}
              category={child}
              allCategories={allCategories}
              selectedIds={selectedIds}
              onSelectionChange={onSelectionChange}
              level={level + 1} // Increase indentation level for children
            />
          ))}
        </ul>
      )}
    </li>
  );
};

function CategorySelector({ categories = [], selectedIds = new Set(), onSelectionChange, isLoading, error }) {
  const handleCategoryToggle = (categoryId) => {
      const newIds = new Set(selectedIds);
      if (newIds.has(categoryId)) { newIds.delete(categoryId); }
      else { newIds.add(categoryId); }
      onSelectionChange(newIds); // Call the prop function passed from parent
  };

  // Find only top-level categories to start rendering
  const topLevelCategories = categories.filter(cat => cat.parentId === null);

  if (isLoading) return <p className="text-sm text-gray-500 p-2">Loading categories...</p>;
  if (error) return <p className="text-red-500 text-sm p-2">{error}</p>;
  if (!isLoading && categories.length === 0) return <p className="text-sm text-gray-500 p-2">No categories found.</p>;

  return (
    // Container for the whole selector
    <div className="border rounded border-gray-300 bg-white p-2 text-sm">
       {topLevelCategories.length > 0 ? (
            <ul className="space-y-1"> {/* Add some space between top-level items */}
                {topLevelCategories.map(category => (
                    <CategoryItem
                        key={category.id}
                        category={category}
                        allCategories={categories}
                        selectedIds={selectedIds}
                        onSelectionChange={handleCategoryToggle}
                        level={0} // Start top-level items at level 0
                    />
                ))}
            </ul>
        ) : (
             <p className="text-sm text-gray-500 p-2">No top-level categories available.</p>
        )}
    </div>
  );
}

export default CategorySelector;