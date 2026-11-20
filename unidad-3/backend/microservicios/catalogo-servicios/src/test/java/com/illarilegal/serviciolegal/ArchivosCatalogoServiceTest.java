package com.illarilegal.serviciolegal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class ArchivosCatalogoServiceTest {
    @TempDir Path directory;
    final ServicioLegalRepository repository = mock(ServicioLegalRepository.class);
    private ServicioLegalEntity resource() {
        var service = new ServicioLegalEntity(); service.setTipoEntrega("ARCHIVO");
        when(repository.findById(1L)).thenReturn(Optional.of(service));
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        return service;
    }
    @Test void conservaLaVersionAnteriorYUsaNombresGenerados() throws Exception {
        var service = resource(); var files = new ArchivosCatalogoService(repository, directory.toString());
        var first = new MockMultipartFile("archivo", "../../file.pdf", "application/pdf", "%PDF-1.4 first".getBytes());
        files.guardar(1L, first); String previous = service.getArchivoVersion();
        files.guardar(1L, new MockMultipartFile("archivo", "%PDF-1.4 second".getBytes()));
        assertNotEquals(previous, service.getArchivoVersion());
        assertEquals("%PDF-1.4 first", Files.readString(directory.resolve(previous + ".pdf")));
        assertTrue(files.disponible(service));
        try (var entries = Files.list(directory)) { assertEquals(2, entries.count()); }
    }
    @Test void rechazaContenidoQueNoEsPdfYSinDejarArchivos() throws Exception {
        resource(); var files = new ArchivosCatalogoService(repository, directory.toString());
        assertThrows(ResponseStatusException.class, () -> files.guardar(1L,
                new MockMultipartFile("archivo", "file.pdf", "application/pdf", "not PDF".getBytes())));
        try (var entries = Files.list(directory)) { assertEquals(0, entries.count()); }
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void noBuscaRutasArbitrariasYLasAsesoriasNoRecibenArchivos() {
        var service = resource(); var files = new ArchivosCatalogoService(repository, directory.toString());
        service.setArchivoVersion("../../private"); assertFalse(files.disponible(service));
        service.setTipoEntrega("ASESORIA");
        assertThrows(ResponseStatusException.class, () -> files.guardar(1L, new MockMultipartFile("archivo", "%PDF-".getBytes())));
    }
}
