package saasagent.saas_agent.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import saasagent.saas_agent.model.Producto;
import saasagent.saas_agent.repository.ProductoRepository;
import saasagent.saas_agent.service.ProductoService;

import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService{

    private final ProductoRepository productoRepository;

    @Override
    public String catalogoPrompt (Long empresaId){
        List<Producto> productos = productoRepository.findByEmpresa_IdAndActivoTrueOrderByNombreAsc(empresaId);
        if(productos.isEmpty()){
            return "No hay productos disponibles por el momento.";
        }
        return productos.stream().map(this::formatearProducto).collect(Collectors.joining("\n"));
    }

    private String formatearProducto(Producto producto){
        String descripcion = producto.getDescripcion() == null ? "" : " " + producto.getDescripcion();

        String precio = producto.getPrecio().setScale(2, RoundingMode.HALF_UP).toPlainString();

        String stock = producto.getStock() > 0 ? "Stock: " + producto.getStock() + " unidades." : "Stock: Agotado." ;

        return "- " + producto.getNombre() + ":" + descripcion + " Precio: S/ " + precio + ". " + stock;
    }
}