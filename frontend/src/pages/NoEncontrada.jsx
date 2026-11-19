import { Link } from 'react-router'

/** Página 404 para cualquier ruta inexistente (path="*"). */
export default function NoEncontrada() {
  return (
    <div className="pagina centrada">
      <h1 className="codigo-404">404</h1>
      <p>La página que buscás no existe.</p>
      <Link className="boton primario" to="/ordenes">
        Volver a las órdenes
      </Link>
    </div>
  )
}
