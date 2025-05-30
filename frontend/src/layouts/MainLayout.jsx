import React from 'react';
import { Outlet } from 'react-router-dom'; // Outlet renders the matched child route
import Header from '../components/Header';
import Sidebar from '../components/Sidebar';
import Footer from '../components/Footer';

function MainLayout() {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      <div className="flex flex-1">
        <Sidebar />
        <main className="flex-grow p-6 bg-gray-100">
          <Outlet /> {/* Child route components will render here */}
        </main>
      </div>
      <Footer />
    </div>
  );
}

export default MainLayout;