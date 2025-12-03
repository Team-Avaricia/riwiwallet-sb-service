package com.avaricia.sb_service.assistant.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/whatsapp")
public class WhatsAppController {

    // Verificación del webhook (requerido por Meta)
    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        
        // Token de verificación que configuras en Meta (inventado por ti, debe coincidir con el de Meta)
        String verifyToken = "mi_secreto_123";
        
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            System.out.println("Webhook verificado correctamente");
            return ResponseEntity.ok(challenge);
        } else {
            return ResponseEntity.status(403).body("Verificación fallida");
        }
    }

    // Recibir mensajes de WhatsApp
    @SuppressWarnings("unchecked")
    @PostMapping("/webhook")
    public ResponseEntity<String> onMessage(@RequestBody Map<String, Object> payload) {
        System.out.println("=== Webhook de WhatsApp recibido ===");
        System.out.println("Payload completo: " + payload);

        try {
            // Navegar por la estructura del payload de WhatsApp Cloud API
            List<Map<String, Object>> entry = (List<Map<String, Object>>) payload.get("entry");
            
            if (entry != null && !entry.isEmpty()) {
                Map<String, Object> firstEntry = entry.get(0);
                List<Map<String, Object>> changes = (List<Map<String, Object>>) firstEntry.get("changes");
                
                if (changes != null && !changes.isEmpty()) {
                    Map<String, Object> change = changes.get(0);
                    Map<String, Object> value = (Map<String, Object>) change.get("value");
                    
                    // Obtener mensajes
                    List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
                    
                    if (messages != null && !messages.isEmpty()) {
                        for (Map<String, Object> message : messages) {
                            // Obtener número de teléfono del remitente
                            String from = (String) message.get("from");
                            
                            // Obtener tipo de mensaje
                            String type = (String) message.get("type");
                            
                            // Obtener el texto del mensaje (si es tipo texto)
                            String text = "";
                            if ("text".equals(type)) {
                                Map<String, Object> textObj = (Map<String, Object>) message.get("text");
                                text = (String) textObj.get("body");
                            }
                            
                            // Imprimir la información
                            System.out.println("========================================");
                            System.out.println("Número: " + from);
                            System.out.println("Tipo de mensaje: " + type);
                            System.out.println("Mensaje: " + text);
                            System.out.println("========================================");
                        }
                    }
                    
                    // También obtener información del contacto
                    List<Map<String, Object>> contacts = (List<Map<String, Object>>) value.get("contacts");
                    if (contacts != null && !contacts.isEmpty()) {
                        Map<String, Object> contact = contacts.get(0);
                        Map<String, Object> profile = (Map<String, Object>) contact.get("profile");
                        String name = profile != null ? (String) profile.get("name") : "Desconocido";
                        System.out.println("Nombre del contacto: " + name);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error procesando mensaje de WhatsApp: " + e.getMessage());
            e.printStackTrace();
        }

        // WhatsApp requiere respuesta 200 OK
        return ResponseEntity.ok("OK");
    }
}
