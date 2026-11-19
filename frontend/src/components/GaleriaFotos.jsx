/**
 * Galería simple de las fotos de la orden (máximo 3).
 * Las rutas son relativas (/uploads/...), servidas por Spring Boot.
 */
export default function GaleriaFotos({ fotos }) {
  if (!fotos || fotos.length === 0) {
    return <p className="estado-info">Sin fotos cargadas.</p>
  }

  return (
    <div className="galeria">
      {fotos.map((ruta, i) => (
        <a key={ruta} href={ruta} target="_blank" rel="noreferrer" title="Abrir en tamaño completo">
          <img src={ruta} alt={`Foto ${i + 1} de la orden`} />
        </a>
      ))}
    </div>
  )
}
