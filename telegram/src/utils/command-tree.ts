export const COMMAND_TREE: Record<string, readonly string[]> = {
  "/seeActivities": [],
  "/myActivities": [],
};

export const COMMAND_PARENT: Record<string, string> = Object.fromEntries(
  Object.entries(COMMAND_TREE).flatMap(([parent, children]) =>
    children.map((child) => [child, parent])
  )
);
/** Comandos de primer nivel que se ofrecen al usuario identificado. */
export const MAIN_MENU: readonly string[] = Object.keys(COMMAND_TREE);

/** Qué hace cada comando del menú principal, para mostrarlo junto al saludo. */
export const COMMAND_DESCRIPTIONS: Record<string, string> = {
  "/seeActivities": "ver las actividades en las que participás",
  "/myActivities": "ver las actividades que organizás",
};

/** Lista los comandos del menú principal con su descripción, uno por línea. El teclado de
 * respuesta puede quedar plegado en algunas apps de Telegram, así que las opciones también van
 * en el texto. */
export function mainMenuText(): string {
  return MAIN_MENU.map((command) => {
    const description = COMMAND_DESCRIPTIONS[command];
    return description ? `${command} — ${description}` : command;
  }).join("\n");
}
