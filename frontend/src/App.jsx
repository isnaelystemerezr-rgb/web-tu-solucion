import { NavLink, Outlet } from 'react-router'

/**
 * Layout principal. El contenido de cada ruta se renderiza en <Outlet/>.
 * La navegación usa NavLink para marcar la sección activa.
 */
export default function App() {
  return (
    <div className="app">
      <header className="topbar">
        <div className="marca">
          <span className="logo">🛠️</span>
          <div>
            <strong>Taller de reparaciones</strong>
            <span className="subtitulo">Órdenes, diagnósticos y entregas</span>
          </div>
        </div>
        <nav className="nav">
          <NavLink to="/ordenes" end>
            Órdenes
          </NavLink>
          <NavLink to="/resumen">Resumen</NavLink>
          <NavLink to="/clientes">Clientes</NavLink>
          <NavLink to="/ordenes/nueva" className="boton primario">
            + Nueva orden
          </NavLink>
        </nav>
      </header>

      <main className="contenido">
        <Outlet />
      </main>

      <footer className="footer">
        Segundo Parcial · Taller de reparaciones — Spring Boot + React
      </footer>
    </div>
  )
}
