package ar.edu.taller.service;

import ar.edu.taller.web.error.ReglaNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Guarda las fotos en disco (carpeta configurable, por defecto ./uploads) y devuelve
 * la ruta relativa "/uploads/<archivo>" que luego se sirve como recurso estático.
 *
 * Se guarda el nombre del archivo en la base, no el binario: así el backup del
 * directorio uploads/ + la base quedan consistentes.
 */
@Service
public class FotoStorageService {

    private static final Set<String> TIPOS_PERMITIDOS =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final Path raiz;

    public FotoStorageService(@Value("${app.uploads.dir:uploads}") String directorio) {
        this.raiz = Paths.get(directorio).toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el directorio de uploads: " + raiz, e);
        }
    }

    public Path getRaiz() {
        return raiz;
    }

    /** Valida y escribe las fotos. Devuelve las rutas relativas en el mismo orden. */
    public List<String> guardar(List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            return List.of();
        }
        return archivos.stream().map(this::guardarUno).toList();
    }

    private String guardarUno(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("ARCHIVO_VACIO", "Se recibió una foto vacía");
        }
        String contentType = archivo.getContentType() == null
                ? "" : archivo.getContentType().toLowerCase(Locale.ROOT);
        if (!TIPOS_PERMITIDOS.contains(contentType)) {
            throw new ReglaNegocioException("ARCHIVO_INVALIDO",
                    "Formato de imagen no permitido: " + contentType + ". Usar JPG, PNG, WEBP o GIF");
        }

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
        String nombre = UUID.randomUUID() + extension;

        try {
            Path destino = raiz.resolve(nombre).normalize();
            // Defensa contra path traversal: el destino debe quedar dentro de la raíz.
            if (!destino.startsWith(raiz)) {
                throw new ReglaNegocioException("ARCHIVO_INVALIDO", "Nombre de archivo inválido");
            }
            try (var in = archivo.getInputStream()) {
                Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + nombre;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la foto", e);
        }
    }
}
