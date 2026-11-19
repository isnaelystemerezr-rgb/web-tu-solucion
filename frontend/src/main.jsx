import React from 'react'
import ReactDOM from 'react-dom/client'
import { createBrowserRouter, RouterProvider } from 'react-router'
import App from './App.jsx'
import OrdenesLista from './pages/OrdenesLista.jsx'
import OrdenNueva from './pages/OrdenNueva.jsx'
import OrdenDetalle from './pages/OrdenDetalle.jsx'
import Resumen from './pages/Resumen.jsx'
import Clientes from './pages/Clientes.jsx'
import NoEncontrada from './pages/NoEncontrada.jsx'
import ErrorDeRuta from './pages/ErrorDeRuta.jsx'
import './styles.css'

/**
 * Rutas anidadas: App es el layout (con <Outlet/>) y cada página es hija.
 * Incluye un parámetro de ruta real: /ordenes/:id
 */
const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    errorElement: <ErrorDeRuta />,
    children: [
      { index: true, element: <OrdenesLista /> },
      { path: 'ordenes', element: <OrdenesLista /> },
      { path: 'ordenes/nueva', element: <OrdenNueva /> },
      { path: 'ordenes/:id', element: <OrdenDetalle /> },
      { path: 'resumen', element: <Resumen /> },
      { path: 'clientes', element: <Clientes /> },
      { path: '*', element: <NoEncontrada /> },
    ],
  },
])

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>,
)
