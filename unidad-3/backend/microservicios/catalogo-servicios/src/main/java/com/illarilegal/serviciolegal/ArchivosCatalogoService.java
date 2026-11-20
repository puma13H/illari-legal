package com.illarilegal.serviciolegal;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ArchivosCatalogoService {
    private final Path root;
    private final ServicioLegalRepository servicios;

    public ArchivosCatalogoService(ServicioLegalRepository servicios,
            @Value("${ILLARI_CATALOGO_ARCHIVOS_DIR:/app/catalogo-archivos}") String directory) {
        this.servicios = servicios;
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    public boolean disponible(ServicioLegalEntity service) {
        String version = service.getArchivoVersion();
        return "guia-bienvenida".equals(version) || version != null
                && version.matches("[a-f0-9-]{36}") && Files.isRegularFile(root.resolve(version + ".pdf"));
    }

    public ServicioLegalEntity guardar(Long id, MultipartFile file) {
        var service = servicios.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!"ARCHIVO".equals(service.getTipoEntrega()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El servicio debe ser un recurso digital.");
        if (file == null || file.isEmpty() || file.getSize() > 10 * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un PDF de hasta 10 MB.");
        Path target = null;
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 5 || !new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo debe ser un PDF válido.");
            String version = UUID.randomUUID().toString();
            Files.createDirectories(root);
            target = root.resolve(version + ".pdf");
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            service.setArchivoVersion(version);
            service.setArchivoNombre("Recurso-ILLARI-LEGAL.pdf");
            return servicios.saveAndFlush(service);
        } catch (IOException | RuntimeException error) {
            if (target != null) try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            if (error instanceof ResponseStatusException status) throw status;
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo guardar el PDF.");
        }
    }

    public Resource obtener(Long id) {
        var service = servicios.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!disponible(service) || "guia-bienvenida".equals(service.getArchivoVersion()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return new FileSystemResource(root.resolve(service.getArchivoVersion() + ".pdf"));
    }
}
