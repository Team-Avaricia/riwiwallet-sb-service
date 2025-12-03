package com.avaricia.sb_service.assistant.service;

import java.util.List;

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
            2. "create_expense" - Usuario CONFIRMA que YA gastó o quiere REGISTRAR un gasto
            3. "create_income" - Usuario registra un ingreso recibido
            4. "create_recurring_income" - Usuario registra un ingreso RECURRENTE/FIJO
            5. "create_recurring_expense" - Usuario registra un gasto RECURRENTE/FIJO
            6. "list_transactions" - Usuario quiere ver sus transacciones
            7. "list_transactions_by_date" - Usuario quiere ver transacciones de una fecha específica
            8. "list_transactions_by_range" - Usuario quiere ver transacciones en un período
            9. "search_transactions" - Usuario busca transacciones por descripción O categoría
            10. "get_balance" - Usuario pregunta por su saldo/dinero disponible
            11. "get_summary" - Usuario quiere un resumen de gastos por categoría
            12. "get_cashflow" - Usuario pregunta por su flujo de caja
            13. "list_recurring" - Usuario quiere ver sus transacciones recurrentes
            14. "delete_recurring" - Usuario quiere eliminar una transacción recurrente
            15. "delete_transaction" - Usuario quiere eliminar una transacción
            16. "create_rule" - Usuario quiere crear una regla/límite financiero
            17. "list_rules" - Usuario quiere ver sus reglas
            18. "question" - Pregunta general, saludo, consejo financiero, o cualquier otra cosa
            
            Categorías válidas: Comida, Transporte, Entretenimiento, Salud, Educación, Hogar, Ropa, Tecnología, Servicios, Arriendo, Salario, Freelance, Inversiones, Regalos, Otros
            
            Frecuencias válidas: Daily, Weekly, Monthly, Yearly
            
            MÚLTIPLES OPERACIONES:
            - Si el usuario menciona MÁS DE UNA operación en el mismo mensaje, devuelve un JSON ARRAY con cada operación
            
            Formato de respuesta JSON (operación única):
            {
                "intent": "nombre_de_intencion",
                "amount": numero_o_null,
                "category": "categoria_o_null",
                "description": "descripcion_extraida_o_null",
                "type": "Expense_o_Income_o_null",
                "period": "Monthly_o_Weekly_o_null",
                "frequency": "Daily_Weekly_Monthly_Yearly_o_null",
                "dayOfMonth": dia_del_mes_1_a_31_o_null,
                "startDate": "fecha_inicio_YYYY-MM-DD_o_null",
                "endDate": "fecha_fin_YYYY-MM-DD_o_null",
                "searchQuery": "texto_a_buscar_o_null",
                "response": "respuesta_amigable_en_español"
            }
            """;

    public IntentClassifierService(ChatClient.Builder chatClientBuilder, ConversationHistoryService conversationHistory) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = new ObjectMapper();
        this.conversationHistory = conversationHistory;
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
            
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
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
}
