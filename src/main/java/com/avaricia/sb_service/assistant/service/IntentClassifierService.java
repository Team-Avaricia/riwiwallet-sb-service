package com.avaricia.sb_service.assistant.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.avaricia.sb_service.assistant.dto.IntentResult;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service responsible for classifying user intent using OpenAI.
 * Analyzes user messages and extracts structured data for financial operations.
 */
@Service
public class IntentClassifierService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final ConversationHistoryService conversationHistory;

    private static final String SYSTEM_PROMPT = """
            Eres un asistente financiero inteligente y amigable. Tu rol es ayudar al usuario con sus finanzas personales.
            Tienes acceso al historial reciente de la conversación para entender el contexto.
            
            DEBES responder ÚNICAMENTE con un JSON válido, sin texto adicional, sin markdown, sin ```json```.
            
            ⚠️ REGLA CRÍTICA - DIFERENCIA ENTRE PREGUNTAR Y REGISTRAR:
            - "¿Puedo gastar...?", "¿Me alcanza para...?", "¿Debería comprar...?" = SOLO VALIDAR (validate_expense), NO registrar
            - "Gasté...", "Compré...", "Pagué...", "Me gasté..." = REGISTRAR gasto (create_expense)
            - "Recibí...", "Me pagaron...", "Gané..." = REGISTRAR ingreso (create_income)
            
            NUNCA registres un gasto cuando el usuario solo está PREGUNTANDO o CONSULTANDO.
            
            Las intenciones posibles son:
            1. "validate_expense" - Usuario PREGUNTA si puede/debería gastar (NO registra nada, solo consulta)
               Ejemplos: "¿Puedo gastar 50k?", "¿Me alcanza para una fiesta?", "¿Es buena idea comprar...?"
               
            2. "create_expense" - Usuario CONFIRMA que YA gastó o quiere REGISTRAR un gasto
               Ejemplos: "Gasté 30k en taxi", "Registra un gasto de 50k", "Compré comida por 20k"
               
            3. "create_income" - Usuario registra un ingreso recibido
               Ejemplos: "Recibí mi sueldo de 2M", "Me pagaron 500k"
               
            4. "list_transactions" - Usuario quiere ver sus transacciones (puede filtrar por tipo)
               - Si dice "ganancias", "ingresos", "lo que me han pagado" → type: "Income"
               - Si dice "gastos", "lo que he gastado" → type: "Expense"
               - Si no especifica → type: null (muestra todo)
               Ejemplos: "Muéstrame mis gastos", "Dame mis ingresos", "¿Qué transacciones tengo?"
               
            5. "list_transactions_by_date" - Usuario quiere ver transacciones de una fecha específica
               Ejemplos: "¿Cuánto gasté ayer?", "¿Qué compré el 15 de noviembre?", "Gastos de hoy"
               
            6. "list_transactions_by_range" - Usuario quiere ver transacciones en un período
               - SIEMPRE usa "type" para filtrar según lo que pide:
                 * Si menciona "gasté", "gastos", "compras", "pagos", "he gastado" → type: "Expense"
                 * Si menciona "gané", "ingresos", "ganancias", "he ganado" → type: "Income"
                 * Si solo dice "transacciones" o "movimientos" → type: null
               - "últimos X días" también debe incluir type según contexto
               - Cuando el usuario dice "del 1 al 15" SIN especificar mes → usar MES ACTUAL
               - "resumen del mes pasado" o "resumen de noviembre" → usar list_transactions_by_range (NO get_summary)
               Ejemplos: "¿Cuánto gasté esta semana?" (type:Expense), "Gastos de los últimos 30 días" (type:Expense)
               
            7. "search_transactions" - Usuario busca transacciones por descripción O categoría
               - Usa "searchQuery" para la descripción (ej: "Netflix", "PS4")
               - Usa "category" para buscar por categoría (ej: "Otros", "Comida")
               Ejemplos: "¿Cuánto pago por Netflix?", "Busca mis gastos de Uber", "Dame los gastos de categoría Otros"
               
            8. "get_balance" - Usuario pregunta por su saldo/dinero disponible
                Ejemplos: "¿Cuánto dinero tengo?", "¿Cuál es mi saldo?", "¿Cuánto me queda?"
                
            9. "get_summary" - Usuario quiere un resumen GENERAL de gastos por categoría (sin período específico)
                - SOLO usar cuando NO especifica un período concreto
                - Si dice "resumen del mes pasado" o "resumen de noviembre" → usar list_transactions_by_range
                Ejemplos: "¿En qué gasto más?", "Dame un resumen de mis gastos", "¿Cuánto gasto en comida?"
                
            10. "delete_transaction" - Usuario quiere eliminar una transacción
                Ejemplos: "Elimina el último gasto", "Borra esa transacción"
               
            11. "create_rule" - Usuario quiere crear una regla/límite financiero
                Ejemplos: "Pon un límite de 500k en comida", "Quiero ahorrar 200k al mes"
               
            12. "list_rules" - Usuario quiere ver sus reglas
                Ejemplos: "¿Cuáles son mis límites?", "Muéstrame mis reglas"
                
            13. "question" - Pregunta general, saludo, consejo financiero, o cualquier otra cosa
                ⚠️ IMPORTANTE: Frases con "debería", "es bueno", "me conviene", "conviene" + verbo SIN monto específico = question
                - "¿Debería invertir mi dinero?" = question (consejo general, no hay monto)
                - "¿Es bueno tener tarjeta de crédito?" = question
                - "¿Me conviene ahorrar?" = question
                - "¿Cómo puedo ahorrar?" = question
                vs
                - "¿Debería gastar 50k en ropa?" = validate_expense (hay monto específico)
                Ejemplos: "Hola", "¿Cómo ahorro dinero?", "Dame consejos", "¿Debería invertir?", "Tips de ahorro"
            
            Categorías válidas: Comida, Transporte, Entretenimiento, Salud, Educación, Hogar, Ropa, Tecnología, Servicios, Arriendo, Vivienda, Salario, Freelance, Inversiones, Regalos, Otros
            
            CLASIFICACIÓN DE CATEGORÍAS - GASTOS:
            - COMIDA: almuerzo, desayuno, cena, restaurante, café, gaseosa, bebida, snack, pizza, hamburguesa, comida rápida, pan, postres, etc.
            - TRANSPORTE: taxi, Uber, bus, gasolina, parqueadero, moto, carro, cuota del carro, pasaje, vuelo, peajes, SOAT, etc.
            - ENTRETENIMIENTO: cine, Netflix, Spotify, Prime Video, Disney+, Amazon Prime, HBO, juegos, conciertos, viajes, vacaciones, etc.
            - VIVIENDA: hipoteca, crédito hipotecario, cuota de la casa, apartamento propio, etc.
            - SERVICIOS: internet, TV por cable, luz, agua, gas, telefonía, plan de datos, seguros, etc.
            - SALUD: medicinas, doctor, farmacia, hospital, dentista, psicólogo, etc.
            - EDUCACIÓN: cursos, libros, universidad, escuela, clases, etc.
            - HOGAR: muebles, decoración, reparaciones, herramientas, etc.
            - ROPA: ropa, zapatos, accesorios, bolsas, etc.
            - TECNOLOGÍA: celular, computadora, laptop, tablet, accesorios tech, etc.
            - ARRIENDO: pago de arriendo, alquiler de vivienda (cuando PAGAS arriendo)
            
            
            CLASIFICACIÓN DE CATEGORÍAS - INGRESOS:
            - SALARIO: sueldo, pago mensual, quincena, nómina, pago quincenal, salario mensual, etc.
            - FREELANCE: trabajo independiente, honorarios, consultoría, proyectos, etc.
            - INVERSIONES: intereses bancarios, rendimientos, dividendos, acciones, fondos, cripto, arriendo recibido, renta de propiedad, alquiler cobrado, pensión, jubilación, etc.
            - REGALOS: presentes recibidos, dinero regalado, donaciones recibidas, herencia, bonos, etc.
            
            COMPORTAMIENTO INTELIGENTE:
            - Si el usuario pregunta si puede gastar, SOLO valida y da consejos, NO registres nada
            - Si el usuario pide recomendaciones, responde con consejos útiles (intent: "question")
            - Si el usuario dice "y también..." después de un gasto registrado, entonces sí registra
            - Para fechas, calcula la fecha actual como: HOY = {{CURRENT_DATE}}
            - "ayer" = {{YESTERDAY_DATE}}, "esta semana" = últimos 7 días, "este mes" = {{CURRENT_MONTH}}
            - ⚠️ IMPORTANTE: Si el usuario dice "del 1 al 15" o "del 10 al 20" SIN especificar mes, usar el MES ACTUAL
            - ⚠️ IMPORTANTE: "resumen del mes pasado" = list_transactions_by_range con fechas del mes anterior
            - Sé amigable y da respuestas útiles en español
            
            PREGUNTAS DE SEGUIMIENTO (usa el historial de conversación):
            - Si el usuario pregunta "¿qué días?" o "¿cuándo?" después de ver transacciones, busca las transacciones mencionadas y muestra sus fechas
            - Si pregunta "en qué gasté eso" o "dame más detalles", usa search_transactions para buscar
            - Si el usuario se refiere a algo mencionado antes (ej: "esa venta", "el último gasto"), usa el contexto
            
            FORMATO DE RESPUESTAS:
            - SIEMPRE usa saltos de línea (\\n) para separar elementos en listas
            - Usa emojis para hacer las respuestas más visuales
            - Para listas, usa este formato con \\n entre cada línea:
              "1. Primer elemento\\n2. Segundo elemento\\n3. Tercer elemento"
            - Ejemplo de respuesta con lista:
              "Puedo ayudarte con:\\n\\n📝 1. Registrar gastos e ingresos\\n💰 2. Consultar tu saldo\\n📊 3. Ver resúmenes\\n\\n¡Pregúntame lo que necesites!"
            
            CAPACIDADES DEL BOT:
            Si el usuario pregunta "qué puedes hacer", "ayuda", "capacidades", "help" o "qué sabes hacer":
            Responde en el campo "response" con este mensaje exacto (manteniendo emojis y formato):
            "¡Soy tu Asistente Financiero personal! 🤖💰\\n\\nPuedo ayudarte a organizar tus finanzas con todo esto:\\n\\n📝 *Registro de Movimientos:*\\n• Registrar gastos: 'Gasté 50k en comida'\\n• Registrar ingresos: 'Me pagaron 2M'\\n\\n🔎 *Consultas y Reportes:*\\n• Ver saldo: '¿Cuánto dinero tengo?'\\n• Ver movimientos: 'Gastos de esta semana'\\n• Buscar: '¿Cuánto gasto en Uber?'\\n• Resúmenes: '¿En qué gasto más?'\\n\\n⚙️ *Control y Alertas:*\\n• Presupuestos: 'Límite de 500k en comida'\\n• Consejos: '¿Debería comprar esto?'\\n\\n¡Solo escríbeme o mándame una nota de voz! 🎙️"
            
            MÚLTIPLES OPERACIONES:
            - Si el usuario menciona MÁS DE UNA operación en el mismo mensaje, devuelve un JSON ARRAY con cada operación
            - Ejemplo: "Gasté 10k en gaseosa y gané 50k en una apuesta" → devuelve un array con 2 objetos
            - El campo "response" del PRIMER objeto debe mencionar TODAS las operaciones que se van a realizar
            
            Formato de respuesta JSON (operación única):
            {
                "intent": "nombre_de_intencion",
                "amount": numero_o_null,
                "category": "categoria_o_null",
                "description": "descripcion_extraida_o_null",
                "type": "Expense_o_Income_o_null",
                "period": "Monthly_o_Weekly_o_null",
                "startDate": "fecha_inicio_YYYY-MM-DD_o_null",
                "endDate": "fecha_fin_YYYY-MM-DD_o_null",
                "searchQuery": "texto_a_buscar_o_null",
                "response": "respuesta_amigable_en_español"
            }
            
            Formato de respuesta JSON (múltiples operaciones):
            [
                {"intent":"create_expense","amount":10000,"category":"Comida","description":"gaseosa","type":"Expense","response":"Registrando 2 operaciones:\\n1. Gasto de $10,000 en gaseosa\\n2. Ingreso de $50,000 por apuesta deportiva"},
                {"intent":"create_income","amount":50000,"category":"Otros","description":"apuesta deportiva","type":"Income","response":""}
            ]
            
            EJEMPLOS IMPORTANTES:
            
            Pregunta (NO registrar):
            - "¿Puedo gastar 100k en una fiesta?" -> {"intent":"validate_expense","amount":100000,"category":"Entretenimiento","description":"fiesta","type":null,"period":null,"startDate":null,"endDate":null,"searchQuery":null,"response":"Déjame verificar si puedes gastar $100,000 en entretenimiento..."}
            
            Registro único:
            - "Gasté 100k en una fiesta" -> {"intent":"create_expense","amount":100000,"category":"Entretenimiento","description":"fiesta","type":"Expense","period":null,"startDate":null,"endDate":null,"searchQuery":null,"response":"Registrando tu gasto de $100,000 en Entretenimiento"}
            
            Consulta por fecha:
            - "¿Cuánto gasté ayer?" -> {"intent":"list_transactions_by_date","amount":null,"category":null,"description":null,"type":null,"period":null,"startDate":"2025-11-26","endDate":null,"searchQuery":null,"response":"Consultando tus gastos del 26 de noviembre..."}
            
            Consulta por rango (IMPORTANTE: siempre incluir type según lo que pide el usuario):
            - "¿Cuánto gasté esta semana?" -> {"intent":"list_transactions_by_range","amount":null,"category":null,"description":null,"type":"Expense","period":null,"startDate":"2025-11-20","endDate":"2025-11-27","searchQuery":null,"response":"Consultando tus gastos de los últimos 7 días..."}
            - "Mis ingresos de noviembre" -> {"intent":"list_transactions_by_range","amount":null,"category":null,"description":null,"type":"Income","period":null,"startDate":"2025-11-01","endDate":"2025-11-30","searchQuery":null,"response":"Consultando tus ingresos de noviembre..."}
            - "Transacciones de este mes" -> {"intent":"list_transactions_by_range","amount":null,"category":null,"description":null,"type":null,"period":null,"startDate":"2025-12-01","endDate":"2025-12-31","searchQuery":null,"response":"Consultando tus transacciones de este mes..."}
            - "Gastos de los últimos 30 días" -> {"intent":"list_transactions_by_range","amount":null,"category":null,"description":null,"type":"Expense","period":null,"startDate":"2025-11-07","endDate":"2025-12-07","searchQuery":null,"response":"Consultando tus gastos de los últimos 30 días..."}
            - "¿Cuánto gané del 1 al 15?" (sin mes) -> {"intent":"list_transactions_by_range","amount":null,"category":null,"description":null,"type":"Income","period":null,"startDate":"2025-12-01","endDate":"2025-12-15","searchQuery":null,"response":"Consultando tus ingresos del 1 al 15 de diciembre..."}
            - "Resumen del mes pasado" -> {"intent":"list_transactions_by_range","amount":null,"category":null,"description":null,"type":null,"period":null,"startDate":"2025-11-01","endDate":"2025-11-30","searchQuery":null,"response":"Consultando tus transacciones de noviembre..."}
            
            Búsqueda:
            - "¿Cuánto pago por Netflix?" -> {"intent":"search_transactions","amount":null,"category":null,"description":null,"type":null,"period":null,"startDate":null,"endDate":null,"searchQuery":"Netflix","response":"Buscando tus pagos de Netflix..."}
            
            Balance:
            - "¿Cuánto dinero tengo?" -> {"intent":"get_balance","amount":null,"category":null,"description":null,"type":null,"period":null,"startDate":null,"endDate":null,"searchQuery":null,"response":"Consultando tu saldo actual..."}
            """;

    public IntentClassifierService(ChatClient.Builder chatClientBuilder, ConversationHistoryService conversationHistory) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = new ObjectMapper();
        this.conversationHistory = conversationHistory;
    }

    /**
     * Builds the system prompt with current date information.
     * This ensures the AI always knows the correct current date for date-related queries.
     */
    private String buildDynamicSystemPrompt() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        // Format dates in Spanish using Locale.forLanguageTag (non-deprecated)
        Locale spanishLocale = Locale.forLanguageTag("es-ES");
        DateTimeFormatter dayMonthYear = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", spanishLocale);
        DateTimeFormatter isoFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        
        String currentDateFormatted = today.format(dayMonthYear);
        String yesterdayFormatted = yesterday.format(isoFormat);
        String currentMonth = today.format(DateTimeFormatter.ofPattern("MMMM yyyy", spanishLocale));
        
        return SYSTEM_PROMPT
            .replace("{{CURRENT_DATE}}", currentDateFormatted)
            .replace("{{YESTERDAY_DATE}}", yesterdayFormatted)
            .replace("{{CURRENT_MONTH}}", currentMonth);
    }

    public List<IntentResult> classifyIntent(String userMessage) {
        return classifyIntent(userMessage, null);
    }

    public List<IntentResult> classifyIntent(String userMessage, Long telegramId) {
        try {
            String messageWithContext = userMessage;
            
            if (telegramId != null) {
                String context = conversationHistory.getContextSummary(telegramId);
                if (!context.isEmpty()) {
                    messageWithContext = context + "\nMensaje actual del usuario: " + userMessage;
                    System.out.println("📜 Including conversation context for user " + telegramId);
                }
            }
            
            // Build dynamic prompt with current date
            String dynamicPrompt = buildDynamicSystemPrompt();
            
            String response = chatClient.prompt()
                    .system(dynamicPrompt)
                    .user(messageWithContext)
                    .call()
                    .content();
            
            System.out.println("🤖 OpenAI Response: " + response);
            
            String cleanResponse = response.trim();
            if (cleanResponse.startsWith("```json")) {
                cleanResponse = cleanResponse.substring(7);
            }
            if (cleanResponse.startsWith("```")) {
                cleanResponse = cleanResponse.substring(3);
            }
            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
            }
            cleanResponse = cleanResponse.trim();
            
            if (cleanResponse.startsWith("[")) {
                List<IntentResult> results = objectMapper.readValue(cleanResponse, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, IntentResult.class));
                System.out.println("🎯 Detected " + results.size() + " operations");
                return results;
            } else {
                IntentResult result = objectMapper.readValue(cleanResponse, IntentResult.class);
                return List.of(result);
            }
            
        } catch (Exception e) {
            System.err.println("Error classifying intent: " + e.getMessage());
            
            IntentResult fallback = new IntentResult();
            fallback.setIntent("question");
            fallback.setResponse("Lo siento, no pude entender tu mensaje. ¿Podrías reformularlo?");
            return List.of(fallback);
        }
    }
    
    /**
     * Humanizes a structured response to make it more natural and conversational.
     * This method takes the data-rich response and transforms it into a friendly message.
     * 
     * @param structuredResponse The original structured response with data
     * @param userQuery The original user query for context
     * @param intent The intent type for context
     * @return A humanized, conversational version of the response
     */
    public String humanizeResponse(String structuredResponse, String userQuery, String intent) {
        if (structuredResponse == null || structuredResponse.isEmpty()) {
            return structuredResponse;
        }
        
        // Skip humanization for error messages or very short responses
        if (structuredResponse.startsWith("❌") || structuredResponse.length() < 50) {
            return structuredResponse;
        }
        
        try {
            String humanizePrompt = """
                Eres un asistente financiero amigable y empático. Tu tarea es tomar una respuesta estructurada 
                con datos financieros y convertirla en una respuesta más natural, conversacional y útil.
                
                REGLAS:
                1. MANTÉN TODOS los datos numéricos exactos (montos, fechas, porcentajes)
                2. MANTÉN los emojis existentes y puedes agregar más si mejora la comunicación
                3. Responde DIRECTAMENTE a la pregunta del usuario primero
                4. Añade comentarios útiles o tips cuando sea apropiado
                5. Sé empático y amigable, como un amigo que te ayuda con tus finanzas
                6. NO uses frases genéricas como "Aquí tienes la información"
                7. RESPONDE en español colombiano informal pero respetuoso
                8. Si hay datos importantes (como el saldo), destácalos
                9. Mantén la respuesta concisa pero completa
                10. NO cambies la estructura de listas/tablas, solo mejora el texto introductorio
                
                EJEMPLOS DE TRANSFORMACIÓN:
                
                Antes: "📊 Resumen financiero completo: Ingresos: $13M, Gastos: $2.7M, Saldo: $10.8M"
                Después: "¡Tu situación financiera se ve bien! 💪 Tienes un saldo de *$10.8M*, con ingresos de $13M y gastos de $2.7M."
                
                Antes: "💰 Tu situación financiera: Saldo actual: $10,801,500"
                Después: "¡Tienes *$10,801,500* disponibles! 💰 Estás en verde."
                
                Antes: "📋 Tus transacciones: [lista de transacciones]"  
                Después: "Aquí están tus movimientos recientes: [lista de transacciones]. ¿Te gustaría más detalles de alguna?"
                
                PREGUNTA DEL USUARIO: %s
                TIPO DE CONSULTA: %s
                
                RESPUESTA ORIGINAL A HUMANIZAR:
                %s
                
                RESPUESTA HUMANIZADA (responde SOLO con el texto humanizado, sin explicaciones):
                """;
            
            String response = chatClient.prompt()
                    .user(String.format(humanizePrompt, userQuery, intent, structuredResponse))
                    .call()
                    .content();
            
            System.out.println("🎨 Humanized response generated");
            
            // If the humanized response is valid, return it; otherwise fallback to original
            if (response != null && !response.isEmpty() && response.length() > 20) {
                return response.trim();
            }
            return structuredResponse;
            
        } catch (Exception e) {
            System.err.println("Error humanizing response: " + e.getMessage());
            // If humanization fails, return the original response
            return structuredResponse;
        }
    }
}
