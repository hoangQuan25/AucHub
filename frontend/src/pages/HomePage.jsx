import React from 'react';
import { useKeycloak } from '@react-keycloak/web';

function HomePage() {
  const { keycloak } = useKeycloak();

  return (
    <div>
      <h1 className="text-3xl font-bold mb-4">Auction Platform Homepage</h1>
      <p className="mb-4">Welcome, {keycloak.tokenParsed?.preferred_username}!</p>

      {/* Khu vực hiển thị danh sách đấu giá sẽ ở đây */}
      <div className="p-10 border mt-4 bg-gray-50">
        <p className="text-gray-500">(Auction list will be displayed here)</p>
         <p className="text-gray-500">Homepage Content - Currently Empty</p>
      </div>

      {/* Phần nâng cấp seller sẽ thêm vào Account Settings sau */}
    </div>
  );
}

export default HomePage;