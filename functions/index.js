const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

// Esta función se activa cuando se escribe una nueva alerta en Firebase
exports.enviarNotificacion = functions.database
    .ref("/salas/{codigoSala}/alertas/{alertaId}")
    .onCreate(async (snapshot, context) => {
        const codigoSala = context.params.codigoSala;
        const alerta = snapshot.val();

        console.log("Nueva alerta en sala:", codigoSala, alerta);

        // Obtener el token del receptor de esta sala
        const tokenSnapshot = await admin.database()
            .ref(`/salas/${codigoSala}/tokenReceptor`)
            .once("value");

        const tokenReceptor = tokenSnapshot.val();

        if (!tokenReceptor) {
            console.log("No hay receptor conectado en esta sala");
            return null;
        }

        // Crear el mensaje de notificación
        const mensaje = {
            notification: {
                title: "👶 Baby Monitor - Alerta",
                body: alerta.tipo || "Sonido detectado"
            },
            data: {
                codigoSala: codigoSala,
                tipo: alerta.tipo || "Sonido detectado",
                hora: alerta.hora || ""
            },
            token: tokenReceptor
        };

        // Enviar la notificación
        try {
            const respuesta = await admin.messaging().send(mensaje);
            console.log("Notificación enviada:", respuesta);
            return respuesta;
        } catch (error) {
            console.log("Error al enviar notificación:", error);
            return null;
        }
    });