class KpiPanel {
    constructor() {
        this.cboEl = document.getElementById('kpi-cbo');
        this.violacionesEl = document.getElementById('kpi-violaciones');
        this.elementosMinEl = document.getElementById('kpi-elementos-min');
        this.tasaCompilacionEl = document.getElementById('kpi-tasa');
        this.advertenciasLista = document.getElementById('kpi-advertencias-lista');
        
        document.getElementById('btn-update-kpi').addEventListener('click', () => {
            if (window.actualizarMetricas) {
                window.actualizarMetricas();
            }
        });
    }

    updateMetrics(dto) {
        // CBO Semaforo
        const cbo = dto.acoplamientoCbo;
        this.cboEl.innerText = cbo.toFixed(2);
        this.cboEl.className = 'kpi-value';
        if (cbo < 5.0) this.cboEl.classList.add('cbo-green');
        else if (cbo <= 8.0) this.cboEl.classList.add('cbo-yellow');
        else this.cboEl.classList.add('cbo-red');

        // Violaciones
        this.violacionesEl.innerText = dto.violacionesUml;
        
        // Productividad
        this.elementosMinEl.innerText = dto.elementosPorMinuto.toFixed(2) + ' elem/min';
        this.tasaCompilacionEl.innerText = dto.tasaCompilacion.toFixed(2) + '%';

        // Advertencias
        this.advertenciasLista.innerHTML = '';
        dto.detallesViolaciones.forEach(adv => {
            const li = document.createElement('li');
            li.innerText = adv;
            this.advertenciasLista.appendChild(li);
        });
    }
}

window.addEventListener('load', () => {
    window.kpiPanel = new KpiPanel();

    window.actualizarMetricas = () => {
        if (!window.sessionToken || !window.proyectoId) return;
        
        const jwt = document.getElementById('jwtInput').value;
        fetch(`/api/v1/proyectos/${window.proyectoId}/metricas/calcular`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwt}`
            }
        })
        .then(res => res.json())
        .then(data => {
            window.kpiPanel.updateMetrics(data);
        })
        .catch(err => console.error("Error actualizando KPIs", err));
    };
});
