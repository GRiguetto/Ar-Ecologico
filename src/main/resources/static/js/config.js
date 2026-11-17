// =====================================================================
// Único lugar para configurar a conexão com o back-end (Spring Boot).
// Abrindo as telas pelo próprio Spring (http://localhost:8080) a API está
// na mesma origem; abrindo por outro servidor (ex.: Live Server), usa a 8080.
// =====================================================================
const URL_API = location.port === '8080' ? '' : 'http://localhost:8080';
