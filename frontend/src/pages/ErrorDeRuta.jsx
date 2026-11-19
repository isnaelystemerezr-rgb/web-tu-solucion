import { Link, useRouteError } from 'react-router'

/** errorElement del router: muestra el error de carga de una ruta. */
export default function ErrorDeRuta() {
  const error = useRouteError()
  return (
    <div className="pagina centrada">
      <h1>Algo salió mal</h1>
      <p className="mini">{error?.message || String(error)}</p>
      <Link className="boton primario" to="/ordenes">
        Volver a las órdenes
      </Link>
    </div>
  )
}
