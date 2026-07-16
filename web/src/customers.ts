// Mirrors src/main/resources/db/migration/V2__seed_data.sql (Galicia demo dataset:
// 3 sales reps, 40 customers). Kept in sync by hand — same ids, names, coordinates
// and priorities as the Java seed so the demo map matches the running API.

export type CustomerPriority = 'LOW' | 'NORMAL' | 'KEY';

export interface SalesRep {
  id: number;
  name: string;
  lat: number;
  lng: number;
}

export interface Customer {
  id: number;
  name: string;
  address: string;
  lat: number;
  lng: number;
  priority: CustomerPriority;
  windowOpen: string | null;
  windowClose: string | null;
  active: boolean;
}

export const SALES_REPS: SalesRep[] = [
  { id: 1, name: 'Laura Castro', lat: 43.0121, lng: -7.5559 },
  { id: 2, name: 'Brais Seoane', lat: 42.5219, lng: -7.5145 },
  { id: 3, name: 'Antía Vázquez', lat: 42.3358, lng: -7.8639 },
];

export const CUSTOMERS: Customer[] = [
  // Lugo city
  { id: 1, name: 'Cafetería Rúa Nova', address: 'Rúa Nova 12, Lugo', lat: 43.0096, lng: -7.556, priority: 'NORMAL', windowOpen: null, windowClose: null, active: true },
  { id: 2, name: 'Estanco Porta Miñá', address: 'Porta Miñá 3, Lugo', lat: 43.008, lng: -7.562, priority: 'NORMAL', windowOpen: '09:30', windowClose: '13:30', active: true },
  { id: 3, name: 'Ferretería As Termas', address: 'Rúa das Termas 5, Lugo', lat: 43.0205, lng: -7.549, priority: 'KEY', windowOpen: null, windowClose: null, active: true },
  { id: 4, name: 'Panadería O Ceao', address: 'Rúa Illa de Sálvora 2, O Ceao, Lugo', lat: 43.041, lng: -7.571, priority: 'LOW', windowOpen: null, windowClose: null, active: true },
  { id: 5, name: 'Quiosco Parque Rosalía', address: 'Parque Rosalía de Castro, Lugo', lat: 43.011, lng: -7.558, priority: 'NORMAL', windowOpen: null, windowClose: null, active: false },

  // Wider Lugo/Ourense province, all >= 15km from the Lugo depot
  { id: 6, name: 'Bazar Monforte Centro', address: 'Rúa Cardenal Rodrigo de Castro 8, Monforte de Lemos', lat: 42.5222, lng: -7.515, priority: 'NORMAL', windowOpen: null, windowClose: null, active: true },
  { id: 7, name: 'Panadería Camiño Francés', address: 'Rúa Maior 22, Sarria', lat: 42.7791, lng: -7.4116, priority: 'KEY', windowOpen: null, windowClose: null, active: true },
  { id: 8, name: 'Ferretería Chantada', address: 'Praza do Concello 4, Chantada', lat: 42.6067, lng: -7.7735, priority: 'NORMAL', windowOpen: '09:00', windowClose: '13:30', active: true },
  { id: 9, name: 'Frutería Valdeorras', address: 'Avenida de Ourense 15, O Barco de Valdeorras', lat: 42.4178, lng: -6.9801, priority: 'LOW', windowOpen: null, windowClose: null, active: true },
  { id: 10, name: 'Librería Pontevella', address: 'Rúa do Paseo 30, Ourense', lat: 42.3382, lng: -7.8618, priority: 'KEY', windowOpen: null, windowClose: null, active: true },
  { id: 11, name: 'Carnicería Terra Cha', address: 'Rúa Pardo de Cela 11, Vilalba', lat: 43.301, lng: -7.6787, priority: 'KEY', windowOpen: null, windowClose: null, active: true },
  { id: 12, name: 'Óptica Ribadeo', address: 'Rúa Reinante 9, Ribadeo', lat: 43.5393, lng: -7.0382, priority: 'NORMAL', windowOpen: null, windowClose: null, active: true },
  { id: 13, name: 'Farmacia Viveiro', address: 'Rúa Nicolás Cora Montenegro 6, Viveiro', lat: 43.6636, lng: -7.5918, priority: 'NORMAL', windowOpen: '16:00', windowClose: '19:30', active: true },
  { id: 14, name: 'Droguería Mondoñedo', address: 'Rúa do Seminario 3, Mondoñedo', lat: 43.4303, lng: -7.3612, priority: 'LOW', windowOpen: null, windowClose: null, active: true },
  { id: 15, name: 'Bar O Cruceiro', address: 'Rúa do Camiño 14, Palas de Rei', lat: 42.8752, lng: -7.8668, priority: 'NORMAL', windowOpen: null, windowClose: null, active: true },
  { id: 16, name: 'Taller Becerreá', address: 'Rúa da Fonte 7, Becerreá', lat: 42.8579, lng: -7.1572, priority: 'NORMAL', windowOpen: null, windowClose: null, active: true },
  { id: 17, name: 'Supermercado A Fonsagrada', address: 'Rúa do Hospital 5, A Fonsagrada', lat: 43.1269, lng: -7.0657, priority: 'NORMAL', windowOpen: '09:00', windowClose: '13:30', active: true },
  { id: 18, name: 'Peluquería Quiroga', address: 'Rúa Antonio López Pérez 2, Quiroga', lat: 42.4775, lng: -7.2686, priority: 'LOW', windowOpen: null, windowClose: null, active: true },
  { id: 19, name: 'Papelería A Rúa', address: 'Avenida de Galicia 18, A Rúa', lat: 42.3953, lng: -7.1099, priority: 'NORMAL', windowOpen: null, windowClose: null, active: true },
  { id: 20, name: 'Zapatería Verín', address: 'Rúa Sousas 10, Verín', lat: 41.9432, lng: -7.4358, priority: 'NORMAL', windowOpen: '16:00', windowClose: '19:30', active: true },
  { id: 21, name: 'Autoescuela Limia', address: 'Rúa Antonio Prada 12, Xinzo de Limia', lat: 42.0653, lng: -7.7223, priority: 'NORMAL', windowOpen: null, windowClose: null, active: true },
  { id: 22, name: 'Gestoría Allariz', address: 'Rúa do Portelo 6, Allariz', lat: 42.1924, lng: -7.7993, priority: 'NORMAL', windowOpen: '09:00', windowClose: '13:30', active: true },
  { id: 23, name: 'Clínica Veterinaria O Carballiño', address: 'Avenida de Ourense 21, O Carballiño', lat: 42.4328, lng: -8.0768, priority: 'KEY', windowOpen: null, windowClose: null, active: true },
  { id: 24, name: 'Cafetería Ribadavia', address: 'Praza Maior 3, Ribadavia', lat: 42.2901, lng: -8.1413, priority: 'LOW', windowOpen: null, windowClose: null, active: true },
  { id: 25, name: 'Restaurante Celanova', address: 'Rúa Vilar 8, Celanova', lat: 42.1542, lng: -7.9548, priority: 'NORMAL', windowOpen: null, windowClose: null, active: true },
  { id: 26, name: 'Estanco Castro Caldelas', address: 'Rúa do Castelo 1, Castro Caldelas', lat: 42.3779, lng: -7.413, priority: 'LOW', windowOpen: null, windowClose: null, active: true },
  { id: 27, name: 'Panadería Portomarín', address: 'Avenida de Sarria 4, Portomarín', lat: 42.8097, lng: -7.6139, priority: 'NORMAL', windowOpen: '16:00', windowClose: '19:30', active: true },
  { id: 28, name: 'Ferretería Monterroso', address: 'Rúa Xoán XXIII 9, Monterroso', lat: 42.7942, lng: -7.8318, priority: 'NORMAL', windowOpen: null, windowClose: null, active: true },
  { id: 29, name: 'Peixería Foz', address: 'Rúa Isidoro Velo 5, Foz', lat: 43.572, lng: -7.2561, priority: 'KEY', windowOpen: null, windowClose: null, active: true },
  { id: 30, name: 'Frutería Burela', address: 'Rúa Marqués de Amboage 7, Burela', lat: 43.6592, lng: -7.3543, priority: 'NORMAL', windowOpen: '09:00', windowClose: '13:30', active: true },
  { id: 31, name: 'Adega Sober', address: 'Rúa da Ribeira Sacra 2, Sober', lat: 42.4632, lng: -7.5858, priority: 'LOW', windowOpen: null, windowClose: null, active: true },
  { id: 32, name: 'Bazar Taboada', address: 'Rúa Principal 11, Taboada', lat: 42.7172, lng: -7.7598, priority: 'NORMAL', windowOpen: null, windowClose: null, active: true },
  { id: 33, name: 'Farmacia Monforte', address: 'Rúa Roberto Baamonde 3, Monforte de Lemos', lat: 42.5195, lng: -7.5122, priority: 'NORMAL', windowOpen: '16:00', windowClose: '19:30', active: true },
  { id: 34, name: 'Papelería Ourense', address: 'Rúa Paz Novoa 16, Ourense', lat: 42.3334, lng: -7.8663, priority: 'NORMAL', windowOpen: null, windowClose: null, active: true },
  { id: 35, name: 'Carnicería Sarria', address: 'Rúa Formigueiro 6, Sarria', lat: 42.7746, lng: -7.4164, priority: 'NORMAL', windowOpen: '09:00', windowClose: '13:30', active: true },
  { id: 36, name: 'Cafetería Terra Cha', address: 'Rúa Ganado 4, Vilalba', lat: 43.2964, lng: -7.6833, priority: 'NORMAL', windowOpen: null, windowClose: null, active: true },
  { id: 37, name: 'Óptica Chantada', address: 'Rúa Otero Pedrayo 9, Chantada', lat: 42.6113, lng: -7.7686, priority: 'LOW', windowOpen: null, windowClose: null, active: true },
  { id: 38, name: 'Droguería Verín', address: 'Rúa Luís Espada 8, Verín', lat: 41.9386, lng: -7.4404, priority: 'NORMAL', windowOpen: '16:00', windowClose: '19:30', active: true },
  { id: 39, name: 'Taller O Carballiño', address: 'Avenida Buenos Aires 14, O Carballiño', lat: 42.4282, lng: -8.0814, priority: 'NORMAL', windowOpen: null, windowClose: null, active: true },
  { id: 40, name: 'Estanco Ribadeo', address: 'Rúa San Roque 5, Ribadeo', lat: 43.5346, lng: -7.0429, priority: 'NORMAL', windowOpen: '09:00', windowClose: '13:30', active: true },
];
