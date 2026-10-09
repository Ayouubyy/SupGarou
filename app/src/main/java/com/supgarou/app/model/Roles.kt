package com.supgarou.app.model

import kotlinx.serialization.Serializable

@Serializable
enum class Team { VILLAGE, WOLVES, SOLO }

/** When a custom role wakes up. */
@Serializable
enum class CustomWake { NEVER, NIGHT_1, EVERY_NIGHT, FROM_NIGHT_2 }

@Serializable
data class RoleDef(
    val id: String,
    val name: String,
    val icon: String,
    val team: Team,
    val wakesEn: String = "",
    val wakesFr: String = "",
    val descEn: String = "",
    val descFr: String = "",
    val custom: Boolean = false,
    val customWake: CustomWake = CustomWake.NEVER,
    val customTargets: Int = 1,
    val customBadge: String = "⭐",
) {
    val wakes: String get() = if (Lang.french) wakesFr.ifBlank { wakesEn } else wakesEn.ifBlank { wakesFr }
    val description: String get() = if (Lang.french) descFr.ifBlank { descEn } else descEn.ifBlank { descFr }
}

fun teamName(team: Team?): String = when (team) {
    Team.VILLAGE -> tr("Village", "Village")
    Team.WOLVES -> tr("Wolves", "Loups")
    Team.SOLO -> tr("Solo", "Solitaire")
    null -> tr("Unknown", "Inconnu")
}

object RoleIds {
    const val VILLAGEOIS = "villageois"
    const val SALVA = "salva"
    const val VOYANTE = "voyante"
    const val OURS = "ours"
    const val GRAND_OURS = "grand_ours"
    const val BERGER = "berger"
    const val SORCIERE = "sorciere"
    const val ANCIEN = "ancien"
    const val RENARD = "renard"
    const val CUPIDON = "cupidon"
    const val CHASSEUR = "chasseur"
    const val VOLEUR = "voleur"
    const val CORBEAU = "corbeau"
    const val JUGE = "juge"
    const val TROLL = "troll"
    const val PETITE_FILLE = "petite_fille"
    const val LOUP = "loup"
    const val LOUP_INFECT = "loup_infect"
    const val LOUP_ROUGE = "loup_rouge"
    const val LOUP_NOIR = "loup_noir"
    const val LOUP_BLANC = "loup_blanc"
    const val FLUTE = "flute"
    const val ALIEN = "alien"

    /** Wolf roles that wake up together for the kill. */
    val PACK = listOf(LOUP, LOUP_INFECT, LOUP_ROUGE, LOUP_NOIR, LOUP_BLANC)
}

object DefaultRoles {
    val all: List<RoleDef> = listOf(
        RoleDef(
            RoleIds.VILLAGEOIS, "Villageois", "👨‍🌾", Team.VILLAGE,
            "Never", "Jamais",
            "No power. Debates and votes during the day.",
            "Aucun pouvoir. Débat et vote pendant la journée.",
        ),
        RoleDef(
            RoleIds.SALVA, "Salva", "🛡️", Team.VILLAGE,
            "Every night", "Chaque nuit",
            "Protects one player from the wolves' attack that night. Can protect himself, but not the same player two nights in a row.",
            "Protège un joueur de l'attaque des loups pour la nuit. Peut se protéger lui-même, mais pas le même joueur deux nuits de suite.",
        ),
        RoleDef(
            RoleIds.VOYANTE, "Voyante", "🔮", Team.VILLAGE,
            "Every night", "Chaque nuit",
            "Looks at one player's card and learns their exact role.",
            "Regarde la carte d'un joueur et découvre son rôle exact.",
        ),
        RoleDef(
            RoleIds.OURS, "Ours", "🐻", Team.VILLAGE,
            "Never (checked each morning)", "Jamais (vérifié chaque matin)",
            "Each morning the narrator growls if one of her two living neighbors is a wolf.",
            "Chaque matin, le narrateur grogne si l'un de ses deux voisins vivants est un loup.",
        ),
        RoleDef(
            RoleIds.GRAND_OURS, "Grand ours", "🧸", Team.VILLAGE,
            "Every night", "Chaque nuit",
            "Picks a player. Next morning the narrator growls if one of that player's two living neighbors is a wolf.",
            "Désigne un joueur. Le lendemain matin, le narrateur grogne si l'un des deux voisins vivants de ce joueur est un loup.",
        ),
        RoleDef(
            RoleIds.BERGER, "Berger", "🐑", Team.VILLAGE,
            "Every night, if he wants", "Chaque nuit, s'il le souhaite",
            "Owns 3 sheep for the whole game and may send one (or more, as long as he has them) to players each night. A sheep sent to any wolf (Loup blanc included) dies for good; otherwise it comes back.",
            "Possède 3 moutons pour toute la partie et peut en envoyer un (ou plusieurs, tant qu'il en a) chaque nuit. Un mouton envoyé chez un loup (Loup blanc compris) meurt pour de bon ; sinon il revient.",
        ),
        RoleDef(
            RoleIds.SORCIERE, "Sorcière", "🧪", Team.VILLAGE,
            "Every night, after the wolves", "Chaque nuit, après les loups",
            "Learns the wolves' victim. Has one life potion (saves the victim) and one death potion (kills anyone), each usable once per game.",
            "Découvre la victime des loups. Possède une potion de vie (sauve la victime) et une potion de mort (tue n'importe qui), chacune utilisable une fois par partie.",
        ),
        RoleDef(
            RoleIds.ANCIEN, "Ancien", "👴", Team.VILLAGE,
            "Last, every night", "En dernier, chaque nuit",
            "Chooses which player starts talking and voting. If the village vote, the Sorcière or the Chasseur kills him, every villager loses their power.",
            "Choisit quel joueur commence à parler et à voter. Si le vote du village, la Sorcière ou le Chasseur le tue, tous les villageois perdent leur pouvoir.",
        ),
        RoleDef(
            RoleIds.RENARD, "Renard", "🦊", Team.VILLAGE,
            "Every night, until he finds a wolf", "Chaque nuit, jusqu'à trouver un loup",
            "Points at a player: if that player or one of their two living neighbors is a wolf, he gets a yes and loses his power. A no keeps it.",
            "Désigne un joueur : si ce joueur ou l'un de ses deux voisins vivants est un loup, il obtient un oui et perd son pouvoir. Un non le lui laisse.",
        ),
        RoleDef(
            RoleIds.CUPIDON, "Cupidon", "💘", Team.VILLAGE,
            "Night 1 only", "Nuit 1 seulement",
            "Names two lovers. If one dies, the other dies of grief at once. A wolf + villager couple plays for itself and wins as the last two alive.",
            "Désigne deux amoureux. Si l'un meurt, l'autre meurt de chagrin aussitôt. Un couple loup + villageois joue pour lui-même et gagne s'il reste seul en vie.",
        ),
        RoleDef(
            RoleIds.CHASSEUR, "Chasseur", "🎯", Team.VILLAGE,
            "When he dies", "À sa mort",
            "When he is eliminated, he immediately shoots a player, who dies too.",
            "Quand il est éliminé, il tire immédiatement sur un joueur, qui meurt aussi.",
        ),
        RoleDef(
            RoleIds.VOLEUR, "Voleur", "🎭", Team.VILLAGE,
            "Night 1 only", "Nuit 1 seulement",
            "Sees the 2 extra cards and may swap his card for one; must take a wolf if both are wolves. Then plays that role.",
            "Voit les 2 cartes en trop et peut échanger sa carte contre l'une d'elles ; doit prendre un loup si les deux sont des loups. Il joue ensuite ce rôle.",
        ),
        RoleDef(
            RoleIds.CORBEAU, "Corbeau", "🐦", Team.VILLAGE,
            "Every night from night 2", "Chaque nuit à partir de la nuit 2",
            "Each night he chooses a player, who starts the next vote with 2 votes against them.",
            "Chaque nuit, il désigne un joueur qui commence le vote suivant avec 2 voix contre lui.",
        ),
        RoleDef(
            RoleIds.JUGE, "Juge", "⚖️", Team.VILLAGE,
            "Every night from night 2", "Chaque nuit à partir de la nuit 2",
            "Chooses a player every night. If that player is voted out the next day, they stay in the game (one-time vote protection).",
            "Désigne un joueur chaque nuit. Si ce joueur est éliminé par le vote le lendemain, il reste en jeu (protection unique contre le vote).",
        ),
        RoleDef(
            RoleIds.TROLL, "Troll", "👺", Team.SOLO,
            "Never", "Jamais",
            "Must get himself voted out by acting suspicious. If the village votes him out, the game is over and he wins.",
            "Doit se faire éliminer par le vote en ayant l'air suspect. Si le village l'élimine, la partie est finie et il gagne.",
        ),
        RoleDef(
            RoleIds.PETITE_FILLE, "Petite fille", "👧", Team.VILLAGE,
            "During the wolves' turn", "Pendant le tour des loups",
            "May spy on the wolves through half-open eyes. If the wolves catch her, she becomes their victim instead and is simply declared dead that night.",
            "Peut espionner les loups en entrouvrant les yeux. Si les loups la surprennent, elle devient leur victime à la place et est simplement déclarée morte cette nuit-là.",
        ),
        RoleDef(
            RoleIds.LOUP, "Loup", "🐺", Team.WOLVES,
            "Every night", "Chaque nuit",
            "The wolves agree on one victim each night.",
            "Les loups-garous se mettent d'accord sur une victime chaque nuit.",
        ),
        RoleDef(
            RoleIds.LOUP_INFECT, "Loup père infecté", "🦠", Team.WOLVES,
            "With the wolves (can infect from night 2)", "Avec les loups (peut infecter à partir de la nuit 2)",
            "Once per game, infects the wolves' victim instead of killing them: the victim survives and secretly joins the wolves.",
            "Une fois par partie, infecte la victime des loups au lieu de la tuer : la victime survit et rejoint secrètement les loups.",
        ),
        RoleDef(
            RoleIds.LOUP_ROUGE, "Loup rouge", "⛔", Team.WOLVES,
            "Every night from night 2", "Chaque nuit à partir de la nuit 2",
            "Before anyone else acts, blocks one player: that player's ability does nothing tonight. Also hunts with the wolves.",
            "Avant tout le monde, bloque un joueur : son pouvoir ne fait rien cette nuit. Chasse aussi avec les loups.",
        ),
        RoleDef(
            RoleIds.LOUP_NOIR, "Loup noir", "🌑", Team.WOLVES,
            "Every night from night 2, with the wolves", "Chaque nuit à partir de la nuit 2, avec les loups",
            "Mutes one player: the next day they can't talk or vote. Also hunts with the wolves.",
            "Rend un joueur muet : le lendemain il ne peut ni parler ni voter. Chasse aussi avec les loups.",
        ),
        RoleDef(
            RoleIds.LOUP_BLANC, "Loup blanc", "❄️", Team.WOLVES,
            "With the wolves", "Avec les loups",
            "Hunts with the wolves. The Voyante, the Ours and the Grand ours don't see him as a wolf.",
            "Chasse avec les loups. La Voyante, l'Ours et le Grand ours ne le voient pas comme un loup.",
        ),
        RoleDef(
            RoleIds.FLUTE, "Joueur de flûte", "🎶", Team.SOLO,
            "Every night", "Chaque nuit",
            "Charms 2 players each night; all charmed players then wake and see each other. Wins when every other living player is charmed. Can't die to the wolves.",
            "Charme 2 joueurs chaque nuit ; les joueurs charmés se réveillent et se reconnaissent. Gagne quand tous les autres joueurs vivants sont charmés. Ne peut pas être tué par les loups.",
        ),
        RoleDef(
            RoleIds.ALIEN, "Alien", "👽", Team.SOLO,
            "Night 1, then any time by day", "Nuit 1, puis à tout moment le jour",
            "Night 1: shows the narrator his secret sign. By day, each time he makes it, everyone sleeps and he names a player and a role. Right: that player is out. Wrong: the Alien is out. No limit. Wins when only he and one other player remain.",
            "Nuit 1 : montre au narrateur son signe secret. Le jour, chaque fois qu'il le fait, tout le monde dort et il désigne un joueur et un rôle. Juste : ce joueur est éliminé. Faux : l'Alien est éliminé. Sans limite. Gagne quand il ne reste que lui et un autre joueur.",
        ),
    )

    val byId: Map<String, RoleDef> = all.associateBy { it.id }
}
