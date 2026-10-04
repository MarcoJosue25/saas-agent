package saasagent.saas_agent.dto;

import saasagent.saas_agent.model.Producto;

import java.math.BigDecimal;
import java.util.List;

public record ResultadoValidacion(List<String> errores, List<Item> items, BigDecimal total) {

    public boolean esValido(){
        return errores.isEmpty();
    }

    public record Item(Producto producto, int cantidad, String talla){

    }
}
