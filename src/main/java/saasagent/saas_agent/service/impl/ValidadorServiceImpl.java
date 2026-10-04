package saasagent.saas_agent.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import saasagent.saas_agent.dto.ItemPedidoRequest;
import saasagent.saas_agent.dto.ResultadoValidacion;
import saasagent.saas_agent.model.Producto;
import saasagent.saas_agent.repository.ProductoRepository;
import saasagent.saas_agent.service.ValidadorService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ValidadorServiceImpl implements ValidadorService {

    private final ProductoRepository productoRepository;

    @Override
    public ResultadoValidacion validar(Long empresaId, List<ItemPedidoRequest> pedidos){
        List<String> errores = new ArrayList<>();
        List<ResultadoValidacion.Item> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        if (pedidos.isEmpty()) {
            errores.add("El pedido no tiene productos.");
        }

        for (ItemPedidoRequest pedido : pedidos) {
            Optional<Producto> encontrado = productoRepository
                    .findByEmpresa_IdAndNombreIgnoreCaseAndActivoTrue(empresaId, pedido.producto());

            if (pedido.cantidad() <= 0) {
                errores.add("La cantidad de " + pedido.producto() + " debe ser mayor que cero.");
            } else if (encontrado.isEmpty()) {
                errores.add("No encontré el producto \"" + pedido.producto() + "\" en el catálogo.");
            } else if (encontrado.get().getStock() < pedido.cantidad()) {
                errores.add("No hay stock suficiente de " + encontrado.get().getNombre()
                        + " (disponible: " + encontrado.get().getStock() + ").");
            } else {
                Producto producto = encontrado.get();
                items.add(new ResultadoValidacion.Item(producto, pedido.cantidad(), pedido.talla()));
                total = total.add(producto.getPrecio().multiply(BigDecimal.valueOf(pedido.cantidad())));
            }
        }
        return new ResultadoValidacion(errores, items, total);
    }
}

