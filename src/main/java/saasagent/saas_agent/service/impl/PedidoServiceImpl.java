package saasagent.saas_agent.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saasagent.saas_agent.dto.ResultadoValidacion;
import saasagent.saas_agent.model.Conversacion;
import saasagent.saas_agent.model.Empresa;
import saasagent.saas_agent.model.Pedido;
import saasagent.saas_agent.model.PedidoItem;
import saasagent.saas_agent.repository.PedidoItemRepository;
import saasagent.saas_agent.repository.PedidoRepository;
import saasagent.saas_agent.repository.ProductoRepository;
import saasagent.saas_agent.service.PedidoService;

@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoItemRepository pedidoItemRepository;
    private final ProductoRepository productoRepository;

    @Override
    @Transactional
    public Pedido registrar(Empresa empresa, Conversacion conversacion, ResultadoValidacion resultado) {
        Pedido pedido = pedidoRepository.save(Pedido.builder().empresa(empresa).conversacion(conversacion).total(resultado.total()).build());
        for (ResultadoValidacion.Item item : resultado.items()){
            // Descuenta solo si todavía hay stock; si otro pedido se adelantó, devuelve 0
            int actualizado = productoRepository.descontarStock(item.producto().getId(), item.cantidad());
            if (actualizado == 0){
                throw new IllegalStateException("No hay suficiente stock en: " + item.producto().getNombre());
            }
            PedidoItem pedidoItem = pedidoItemRepository.save(PedidoItem.builder().pedido(pedido).producto(item.producto()).cantidad(item.cantidad())
                    .talla(item.talla()).precioUnitario(item.producto().getPrecio()).build());
        }
        return pedido;
    }
}