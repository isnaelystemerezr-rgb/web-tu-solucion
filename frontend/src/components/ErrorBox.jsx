export default function ErrorBox({ mensaje, onReintentar }) {
  if (!mensaje) return null
  return (
    <div className="error-box" role="alert">
      <span>⚠️ {mensaje}</span>
      {onReintentar && (
        <button type="button" className="boton" onClick={onReintentar}>
          Reintentar
        </button>
      )}
    </div>
  )
}
