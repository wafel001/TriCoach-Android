package com.pixelempire.clicker;

import java.util.Locale;

public final class L10n {
    private L10n() {}

    public static final String[] LANGS = {"pl", "en", "es", "cs", "ru", "zh"};
    public static final String[] LANG_LABELS = {"Polski", "English", "Español", "Čeština", "Русский", "中文"};

    public static String nextLang(String lang) {
        for (int i = 0; i < LANGS.length; i++) {
            if (LANGS[i].equals(lang)) return LANGS[(i + 1) % LANGS.length];
        }
        return "pl";
    }

    public static String languageLabel(String lang) {
        for (int i = 0; i < LANGS.length; i++) if (LANGS[i].equals(lang)) return LANG_LABELS[i];
        return LANG_LABELS[0];
    }

    private static int ix(String lang) {
        if ("en".equals(lang)) return 1;
        if ("es".equals(lang)) return 2;
        if ("cs".equals(lang)) return 3;
        if ("ru".equals(lang)) return 4;
        if ("zh".equals(lang)) return 5;
        return 0;
    }

    private static String p(String lang, String pl, String en, String es, String cs, String ru, String zh) {
        switch (ix(lang)) {
            case 1: return en;
            case 2: return es;
            case 3: return cs;
            case 4: return ru;
            case 5: return zh;
            default: return pl;
        }
    }

    public static String t(String lang, String key) {
        switch (key) {
            case "world": return p(lang,"ŚWIAT","WORLD","MUNDO","SVĚT","МИР","世界");
            case "upgrades": return p(lang,"ULEPSZENIA","UPGRADES","MEJORAS","VYLEPŠENÍ","УЛУЧШЕНИЯ","升级");
            case "missions": return p(lang,"MISJE","MISSIONS","MISIONES","MISE","ЗАДАНИЯ","任务");
            case "research": return p(lang,"BADANIA","RESEARCH","INVESTIGACIÓN","VÝZKUM","ИССЛЕДОВАНИЯ","研究");
            case "menu": return p(lang,"MENU","MENU","MENÚ","MENU","МЕНЮ","菜单");
            case "coins": return p(lang,"MONETY","COINS","MONEDAS","MINCE","МОНЕТЫ","金币");
            case "crystals": return p(lang,"KRYSZTAŁY","CRYSTALS","CRISTALES","KRYSTALY","КРИСТАЛЛЫ","水晶");
            case "legacy": return p(lang,"DZIEDZICTWO","LEGACY","LEGADO","ODKAZ","НАСЛЕДИЕ","传承");
            case "per_sec": return p(lang,"/ sek.","/ sec","/ s","/ s","/ сек","/秒");
            case "tap_anywhere": return p(lang,"KLIKNIJ GDZIEKOLWIEK NA ŚWIECIE","TAP ANYWHERE IN THE WORLD","TOCA EN CUALQUIER LUGAR","KLEPNI KDEKOLI VE SVĚTĚ","НАЖМИ В ЛЮБОМ МЕСТЕ","点击世界任意位置");
            case "level": return p(lang,"POZIOM","LEVEL","NIVEL","ÚROVEŇ","УРОВЕНЬ","等级");
            case "stage": return p(lang,"ETAP","STAGE","ETAPA","ETAPA","ЭТАП","阶段");
            case "build_progress": return p(lang,"POSTĘP BUDOWY","BUILD PROGRESS","PROGRESO DE CONSTRUCCIÓN","POSTUP STAVBY","ПРОГРЕСС СТРОЙКИ","建造进度");
            case "next_level": return p(lang,"DO NASTĘPNEGO POZIOMU","TO NEXT LEVEL","HASTA EL SIGUIENTE NIVEL","DO DALŠÍ ÚROVNĚ","ДО СЛЕДУЮЩЕГО УРОВНЯ","距下一级");
            case "click_power": return p(lang,"SIŁA KLIKNIĘCIA","TAP POWER","PODER DE TOQUE","SÍLA KLEPNUTÍ","СИЛА НАЖАТИЯ","点击力量");
            case "auto_income": return p(lang,"DOCHÓD AUTO","AUTO INCOME","INGRESO AUTO","AUTO PŘÍJEM","АВТО ДОХОД","自动收益");
            case "combo": return p(lang,"COMBO","COMBO","COMBO","KOMBO","КОМБО","连击");
            case "critical": return p(lang,"KRYTYK!","CRITICAL!","¡CRÍTICO!","KRITICKÝ!","КРИТ!","暴击！");
            case "buy": return p(lang,"KUP","BUY","COMPRAR","KOUPIT","КУПИТЬ","购买");
            case "owned": return p(lang,"POSIADASZ","OWNED","TIENES","VLASTNÍŠ","КУПЛЕНО","已拥有");
            case "lvl": return p(lang,"poz.","lvl","nv.","úr.","ур.","级");
            case "claim": return p(lang,"ODBIERZ","CLAIM","COBRAR","VYZVEDNOUT","ЗАБРАТЬ","领取");
            case "claimed": return p(lang,"ODEBRANO","CLAIMED","COBRADO","VYZVEDNUTO","ПОЛУЧЕНО","已领取");
            case "daily": return p(lang,"NAGRODA DZIENNA","DAILY REWARD","RECOMPENSA DIARIA","DENNÍ ODMĚNA","ЕЖЕДНЕВНАЯ НАГРАДА","每日奖励");
            case "streak": return p(lang,"seria","streak","racha","série","серия","连续");
            case "achievement": return p(lang,"OSIĄGNIĘCIE","ACHIEVEMENT","LOGRO","ÚSPĚCH","ДОСТИЖЕНИЕ","成就");
            case "mission": return p(lang,"MISJA","MISSION","MISIÓN","MISE","ЗАДАНИЕ","任务");
            case "reward": return p(lang,"NAGRODA","REWARD","RECOMPENSA","ODMĚNA","НАГРАДА","奖励");
            case "completed": return p(lang,"UKOŃCZONO","COMPLETED","COMPLETADO","HOTOVO","ВЫПОЛНЕНО","完成");
            case "research_points": return p(lang,"PUNKTY BADAŃ","RESEARCH POINTS","PUNTOS DE INVESTIGACIÓN","BODY VÝZKUMU","ОЧКИ ИССЛЕДОВАНИЙ","研究点");
            case "unlock": return p(lang,"ODBLOKUJ","UNLOCK","DESBLOQUEAR","ODEMKNOUT","ОТКРЫТЬ","解锁");
            case "locked": return p(lang,"ZABLOKOWANE","LOCKED","BLOQUEADO","ZAMČENO","ЗАКРЫТО","未解锁");
            case "prestige": return p(lang,"ODRODZENIE","ASCENSION","ASCENSIÓN","VZESTUP","ВОЗНЕСЕНИЕ","飞升");
            case "prestige_desc": return p(lang,"Zacznij nową cywilizację z trwałym bonusem.","Start a new civilization with a permanent bonus.","Inicia una nueva civilización con un bonus permanente.","Začni novou civilizaci s trvalým bonusem.","Начни новую цивилизацию с постоянным бонусом.","以永久加成开启新的文明。");
            case "prestige_ready": return p(lang,"GOTOWE DO ODRODZENIA","READY TO ASCEND","LISTO PARA ASCENDER","PŘIPRAVENO K VZESTUPU","ГОТОВО К ВОЗНЕСЕНИЮ","可飞升");
            case "prestige_need": return p(lang,"Dotrzyj co najmniej do etapu 8.","Reach at least stage 8.","Alcanza al menos la etapa 8.","Dosáhni alespoň etapy 8.","Достигни как минимум 8 этапа.","至少到达第8阶段。");
            case "sound": return p(lang,"DŹWIĘK","SOUND","SONIDO","ZVUK","ЗВУК","声音");
            case "haptics": return p(lang,"WIBRACJE","HAPTICS","VIBRACIÓN","VIBRACE","ВИБРАЦИЯ","震动");
            case "language": return p(lang,"JĘZYK","LANGUAGE","IDIOMA","JAZYK","ЯЗЫК","语言");
            case "notation": return p(lang,"SKRÓTY LICZB","NUMBER FORMAT","FORMATO DE NÚMEROS","FORMÁT ČÍSEL","ФОРМАТ ЧИСЕЛ","数字格式");
            case "low_power": return p(lang,"TRYB OSZCZĘDNY","LOW POWER MODE","MODO AHORRO","ÚSPORNÝ REŽIM","ЭКО РЕЖИМ","省电模式");
            case "stats": return p(lang,"STATYSTYKI","STATISTICS","ESTADÍSTICAS","STATISTIKY","СТАТИСТИКА","统计");
            case "total_earned": return p(lang,"Łącznie zarobione","Total earned","Total ganado","Celkem vyděláno","Всего заработано","总收益");
            case "total_taps": return p(lang,"Łącznie kliknięć","Total taps","Toques totales","Celkem klepnutí","Всего нажатий","总点击");
            case "best_combo": return p(lang,"Najlepsze combo","Best combo","Mejor combo","Nejlepší kombo","Лучшее комбо","最高连击");
            case "play_time": return p(lang,"Czas gry","Play time","Tiempo de juego","Doba hraní","Время игры","游戏时间");
            case "ascensions": return p(lang,"Odrodzenia","Ascensions","Ascensiones","Vzestupy","Вознесения","飞升次数");
            case "reset": return p(lang,"WYCZYŚĆ ZAPIS","RESET SAVE","BORRAR PARTIDA","SMAZAT ULOŽENÍ","СБРОСИТЬ СОХРАНЕНИЕ","清除存档");
            case "reset_confirm": return p(lang,"DOTKNIJ PONOWNIE — USUŃ ZAPIS","TAP AGAIN — DELETE SAVE","TOCA OTRA VEZ — BORRAR","KLEPNI ZNOVU — SMAZAT","НАЖМИ ЕЩЁ РАЗ — УДАЛИТЬ","再次点击—删除存档");
            case "offline": return p(lang,"DOCHÓD OFFLINE","OFFLINE INCOME","INGRESO SIN CONEXIÓN","OFFLINE PŘÍJEM","ОФЛАЙН ДОХОД","离线收益");
            case "welcome_back": return p(lang,"WITAJ Z POWROTEM","WELCOME BACK","BIENVENIDO DE NUEVO","VÍTEJ ZPĚT","С ВОЗВРАЩЕНИЕМ","欢迎回来");
            case "away": return p(lang,"Nie było Cię","You were away","Estuviste fuera","Byl jsi pryč","Тебя не было","离线时间");
            case "continue": return p(lang,"GRAJ DALEJ","CONTINUE","CONTINUAR","POKRAČOVAT","ПРОДОЛЖИТЬ","继续");
            case "new_stage": return p(lang,"NOWA BUDOWLA!","NEW BUILDING!","¡NUEVO EDIFICIO!","NOVÁ STAVBA!","НОВОЕ ЗДАНИЕ!","新建筑！");
            case "new_level": return p(lang,"NOWY POZIOM!","LEVEL UP!","¡NUEVO NIVEL!","NOVÁ ÚROVEŇ!","НОВЫЙ УРОВЕНЬ!","升级！");
            case "tutorial_title": return p(lang,"PIXEL EMPIRE","PIXEL EMPIRE","PIXEL EMPIRE","PIXEL EMPIRE","PIXEL EMPIRE","PIXEL EMPIRE");
            case "tutorial_1": return p(lang,"Klikaj na CAŁEJ scenie. Każde dotknięcie buduje świat.","Tap ANYWHERE on the scene. Every tap builds your world.","Toca EN CUALQUIER LUGAR. Cada toque construye tu mundo.","Klepej KDEKOLI. Každé klepnutí buduje tvůj svět.","Нажимай В ЛЮБОМ МЕСТЕ. Каждое нажатие строит мир.","点击场景任意位置。每次点击都会建设你的世界。");
            case "tutorial_2": return p(lang,"Kupuj ulepszenia i automatyzację, aby rosnąć także bez klikania.","Buy upgrades and automation to grow even without tapping.","Compra mejoras y automatización para crecer sin tocar.","Kupuj vylepšení a automatizaci a růst i bez klepání.","Покупай улучшения и автоматизацию для пассивного роста.","购买升级和自动化，即使不点击也能成长。");
            case "tutorial_3": return p(lang,"Co 12 poziomów powstaje zupełnie nowa budowla. Jest ich 24.","Every 12 levels a completely new building appears. There are 24.","Cada 12 niveles aparece un edificio totalmente nuevo. Hay 24.","Každých 12 úrovní vznikne úplně nová stavba. Je jich 24.","Каждые 12 уровней появляется новое здание. Всего их 24.","每12级都会出现全新建筑，共24座。");
            case "tutorial_4": return p(lang,"Misje, badania i Odrodzenie otwierają kolejne warstwy rozwoju.","Missions, research and Ascension unlock deeper progression.","Misiones, investigación y Ascensión abren nuevas capas.","Mise, výzkum a Vzestup otevírají další vrstvy.","Задания, исследования и Вознесение открывают новые слои.","任务、研究与飞升会开启更深层成长。");
            case "tap_continue": return p(lang,"DOTKNIJ, ABY DALEJ","TAP TO CONTINUE","TOCA PARA CONTINUAR","KLEPNI PRO POKRAČOVÁNÍ","НАЖМИ, ЧТОБЫ ПРОДОЛЖИТЬ","点击继续");
            case "event_gold": return p(lang,"ZŁOTY DESZCZ — KLIKNIJ!","GOLD RUSH — TAP!","FIEBRE DEL ORO — ¡TOCA!","ZLATÁ HOREČKA — KLEPNI!","ЗОЛОТАЯ ЛИХОРАДКА — ЖМИ!","淘金热—点击！");
            case "event_crystal": return p(lang,"KRYSZTAŁOWA KOMETA — KLIKNIJ!","CRYSTAL COMET — TAP!","COMETA DE CRISTAL — ¡TOCA!","KRYSTALOVÁ KOMETA — KLEPNI!","КРИСТАЛЬНАЯ КОМЕТА — ЖМИ!","水晶彗星—点击！");
            case "event_frenzy": return p(lang,"SZAŁ BUDOWY — x3 KLIK","BUILD FRENZY — x3 TAP","FRENESÍ DE OBRA — x3","STAVEBNÍ ŠÍLENSTVÍ — x3","СТРОИТЕЛЬНЫЙ АЖИОТАЖ — x3","建造狂潮—点击x3");
            case "event_auto": return p(lang,"NOCNA ZMIANA — x2 AUTO","NIGHT SHIFT — x2 AUTO","TURNO NOCTURNO — x2 AUTO","NOČNÍ SMĚNA — x2 AUTO","НОЧНАЯ СМЕНА — x2 АВТО","夜班—自动x2");
            case "event_xp": return p(lang,"PLAN ARCHITEKTA — x4 XP","ARCHITECT PLAN — x4 XP","PLANO DEL ARQUITECTO — x4 XP","PLÁN ARCHITEKTA — x4 XP","ПЛАН АРХИТЕКТОРА — x4 XP","建筑师蓝图—经验x4");
            default: return key;
        }
    }

    public static String stageName(String lang, int s) {
        String[][] n = {
                {"Szałas z gałęzi","Lean-to Shelter","Refugio de ramas","Přístřešek z větví","Шалаш из веток","树枝棚屋"},
                {"Mała chatka","Tiny Hut","Choza pequeña","Malá chatrč","Маленькая хижина","小木棚"},
                {"Drewniana chata","Wooden Cabin","Cabaña de madera","Dřevěná chata","Деревянный дом","木屋"},
                {"Gospodarstwo","Homestead","Granja","Usedlost","Усадьба","农庄"},
                {"Osada","Settlement","Asentamiento","Osada","Поселение","聚落"},
                {"Wioska","Village","Aldea","Vesnice","Деревня","村庄"},
                {"Ratusz","Town Hall","Ayuntamiento","Radnice","Ратуша","市政厅"},
                {"Kamienna twierdza","Stone Fortress","Fortaleza de piedra","Kamenná pevnost","Каменная крепость","石堡"},
                {"Wielki zamek","Grand Castle","Gran castillo","Velký hrad","Большой замок","宏伟城堡"},
                {"Cytadela","Citadel","Ciudadela","Citadela","Цитадель","要塞"},
                {"Pałac królewski","Royal Palace","Palacio real","Královský palác","Королевский дворец","王宫"},
                {"Miasto kupieckie","Merchant City","Ciudad mercante","Kupecké město","Торговый город","商贸之城"},
                {"Metropolia parowa","Steam Metropolis","Metrópolis de vapor","Parní metropole","Паровой мегаполис","蒸汽都市"},
                {"Wieża elektryczna","Electric Tower","Torre eléctrica","Elektrická věž","Электрическая башня","电力塔"},
                {"Megafabryka","Megafactory","Megafábrica","Megatovárna","Мегафабрика","超级工厂"},
                {"Neonowe miasto","Neon City","Ciudad neón","Neonové město","Неоновый город","霓虹都市"},
                {"Cyberforteca","Cyber Fortress","Fortaleza cibernética","Kyberpevnost","Киберкрепость","赛博堡垒"},
                {"Arcologia","Arcology","Arcología","Arkologická věž","Аркология","生态巨构"},
                {"Miasto w chmurach","Cloud City","Ciudad de las nubes","Město v oblacích","Город в облаках","云端之城"},
                {"Port orbitalny","Orbital Port","Puerto orbital","Orbitální přístav","Орбитальный порт","轨道港"},
                {"Kolonia księżycowa","Lunar Colony","Colonia lunar","Měsíční kolonie","Лунная колония","月球殖民地"},
                {"Pierścień planetarny","Planetary Ring","Anillo planetario","Planetární prstenec","Планетарное кольцо","行星环城"},
                {"Kwantowa cytadela","Quantum Citadel","Ciudadela cuántica","Kvantová citadela","Квантовая цитадель","量子要塞"},
                {"Wieża Nieskończoności","Infinity Spire","Aguja del infinito","Věž nekonečna","Башня Бесконечности","无限尖塔"}
        };
        s = Math.max(0, Math.min(n.length - 1, s));
        return n[s][ix(lang)];
    }

    public static String upgradeName(String lang, String id) {
        switch (id) {
            case "hands": return p(lang,"Sprawne dłonie","Skilled Hands","Manos hábiles","Šikovné ruce","Умелые руки","巧手");
            case "tools": return p(lang,"Lepsze narzędzia","Better Tools","Mejores herramientas","Lepší nářadí","Лучшие инструменты","精良工具");
            case "hammer": return p(lang,"Młot mistrza","Master Hammer","Martillo maestro","Mistrovské kladivo","Молот мастера","大师之锤");
            case "blueprint": return p(lang,"Projekt architekta","Architect Blueprint","Plano del arquitecto","Plán architekta","Чертёж архитектора","建筑蓝图");
            case "crew": return p(lang,"Ekipa budowlana","Builder Crew","Cuadrilla","Stavební četa","Бригада","施工队");
            case "workshop": return p(lang,"Warsztat","Workshop","Taller","Dílna","Мастерская","工坊");
            case "mill": return p(lang,"Tartak","Sawmill","Aserradero","Pila","Лесопилка","锯木厂");
            case "quarry": return p(lang,"Kamieniołom","Quarry","Cantera","Lom","Карьер","采石场");
            case "foundry": return p(lang,"Odlewnia","Foundry","Fundición","Slévárna","Литейная","铸造厂");
            case "factory": return p(lang,"Fabryka modułów","Module Factory","Fábrica modular","Továrna modulů","Модульный завод","模块工厂");
            case "robots": return p(lang,"Roboty konstrukcyjne","Construction Robots","Robots de obra","Stavební roboti","Строительные роботы","建筑机器人");
            case "nanites": return p(lang,"Nanity budowlane","Builder Nanites","Nanitas constructoras","Stavební naniti","Строительные наниты","建筑纳米群");
            case "logistics": return p(lang,"Sieć logistyczna","Logistics Network","Red logística","Logistická síť","Логистическая сеть","物流网络");
            case "drones": return p(lang,"Drony transportowe","Cargo Drones","Drones de carga","Nákladní drony","Грузовые дроны","运输无人机");
            case "ai": return p(lang,"AI zarządcy","Manager AI","IA gestora","Řídicí AI","ИИ-управляющий","管理AI");
            case "fusion": return p(lang,"Reaktor fuzyjny","Fusion Reactor","Reactor de fusión","Fúzní reaktor","Термоядерный реактор","聚变反应堆");
            case "orbital": return p(lang,"Stocznia orbitalna","Orbital Shipyard","Astillero orbital","Orbitální loděnice","Орбитальная верфь","轨道船坞");
            case "quantum": return p(lang,"Konstruktor kwantowy","Quantum Constructor","Constructor cuántico","Kvantový konstruktor","Квантовый конструктор","量子建造器");
            default: return id;
        }
    }

    public static String upgradeDesc(String lang, boolean tap, double value) {
        if (tap) return p(lang,"+"+fmt(value)+" do kliknięcia","+"+fmt(value)+" tap power","+"+fmt(value)+" al toque","+"+fmt(value)+" síla klepnutí","+"+fmt(value)+" к нажатию","点击 +"+fmt(value));
        return p(lang,"+"+fmt(value)+" / sek.","+"+fmt(value)+" / sec","+"+fmt(value)+" / s","+"+fmt(value)+" / s","+"+fmt(value)+" / сек","每秒 +"+fmt(value));
    }

    public static String researchName(String lang, String id) {
        switch (id) {
            case "r1": return p(lang,"Ergonomia","Ergonomics","Ergonomía","Ergonomie","Эргономика","人体工学");
            case "r2": return p(lang,"Prefabrykacja","Prefabrication","Prefabricación","Prefabrikace","Префабрикация","预制技术");
            case "r3": return p(lang,"Lepsze kontrakty","Better Contracts","Mejores contratos","Lepší smlouvy","Лучшие контракты","优质合同");
            case "r4": return p(lang,"Szkolenie ekip","Crew Training","Formación de equipos","Výcvik čet","Обучение бригад","团队训练");
            case "r5": return p(lang,"Lean construction","Lean Construction","Construcción Lean","Lean stavba","Бережливое строительство","精益建造");
            case "r6": return p(lang,"Automatyczne magazyny","Automated Warehouses","Almacenes automáticos","Automatické sklady","Автосклады","自动仓储");
            case "r7": return p(lang,"Druk 3D","3D Printing","Impresión 3D","3D tisk","3D-печать","3D打印");
            case "r8": return p(lang,"Materiały inteligentne","Smart Materials","Materiales inteligentes","Chytré materiály","Умные материалы","智能材料");
            case "r9": return p(lang,"Sieć neuronowa","Neural Network","Red neuronal","Neuronová síť","Нейросеть","神经网络");
            case "r10": return p(lang,"Energia fuzyjna","Fusion Energy","Energía de fusión","Fúzní energie","Термоядерная энергия","聚变能源");
            case "r11": return p(lang,"Grawitacja lokalna","Local Gravity","Gravedad local","Lokální gravitace","Локальная гравитация","局部重力");
            case "r12": return p(lang,"Druk materii","Matter Printing","Impresión de materia","Tisk hmoty","Печать материи","物质打印");
            case "r13": return p(lang,"Pole czasowe","Time Field","Campo temporal","Časové pole","Временное поле","时间场");
            case "r14": return p(lang,"Replikacja","Replication","Replicación","Replikace","Репликация","复制技术");
            case "r15": return p(lang,"Architekt kwantowy","Quantum Architect","Arquitecto cuántico","Kvantový architekt","Квантовый архитектор","量子建筑师");
            case "r16": return p(lang,"Nieskończona energia","Infinite Energy","Energía infinita","Nekonečná energie","Бесконечная энергия","无限能源");
            default: return id;
        }
    }

    public static String researchDesc(String lang, int kind, double bonus) {
        String pct = String.format(Locale.US, "%.0f%%", bonus * 100.0);
        if (kind == 0) return p(lang,"Kliknięcia +"+pct,"Tap power +"+pct,"Toques +"+pct,"Klepnutí +"+pct,"Нажатия +"+pct,"点击 +"+pct);
        if (kind == 1) return p(lang,"Dochód auto +"+pct,"Auto income +"+pct,"Ingreso auto +"+pct,"Auto příjem +"+pct,"Авто доход +"+pct,"自动收益 +"+pct);
        if (kind == 2) return p(lang,"Cały dochód +"+pct,"All income +"+pct,"Todos los ingresos +"+pct,"Veškerý příjem +"+pct,"Весь доход +"+pct,"全部收益 +"+pct);
        return p(lang,"XP budowy +"+pct,"Build XP +"+pct,"XP de obra +"+pct,"Stavební XP +"+pct,"Опыт стройки +"+pct,"建造经验 +"+pct);
    }

    private static String fmt(double d) {
        if (d >= 1_000_000) return String.format(Locale.US,"%.1fM",d/1_000_000d);
        if (d >= 1_000) return String.format(Locale.US,"%.1fK",d/1_000d);
        if (Math.abs(d - Math.rint(d)) < 0.001) return Long.toString((long)Math.rint(d));
        return String.format(Locale.US,"%.1f",d);
    }
}
