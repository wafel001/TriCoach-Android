package com.pixelempire.clicker;

public final class V3L10n {
    private V3L10n(){}
    public static final String[] LANGS={"pl","en","es","cs","ru","zh"};
    public static String next(String lang){for(int i=0;i<LANGS.length;i++)if(LANGS[i].equals(lang))return LANGS[(i+1)%LANGS.length];return "pl";}
    public static String languageName(String lang){switch(lang){case"en":return"English";case"es":return"Español";case"cs":return"Čeština";case"ru":return"Русский";case"zh":return"中文";default:return"Polski";}}
    private static String p(String l,String pl,String en,String es,String cs,String ru,String zh){switch(l){case"en":return en;case"es":return es;case"cs":return cs;case"ru":return ru;case"zh":return zh;default:return pl;}}

    public static String t(String l,String k){switch(k){
        case"world":return p(l,"Świat","World","Mundo","Svět","Мир","世界");
        case"build":return p(l,"Budowa","Build","Construir","Stavba","Стройка","建造");
        case"heroes":return p(l,"Bohaterowie","Heroes","Héroes","Hrdinové","Герои","英雄");
        case"research":return p(l,"Badania","Research","Investigación","Výzkum","Исследования","研究");
        case"missions":return p(l,"Misje","Missions","Misiones","Mise","Миссии","任务");
        case"empire":return p(l,"Imperium","Empire","Imperio","Říše","Империя","帝国");
        case"coins":return p(l,"Monety","Coins","Monedas","Mince","Монеты","金币");
        case"crystals":return p(l,"Kryształy","Crystals","Cristales","Krystaly","Кристаллы","水晶");
        case"science":return p(l,"Punkty badań","Research points","Puntos de investigación","Body výzkumu","Очки исследований","研究点");
        case"legacy":return p(l,"Gwiazdy Dziedzictwa","Legacy Stars","Estrellas de legado","Hvězdy odkazu","Звёзды наследия","传承之星");
        case"level":return p(l,"Poziom","Level","Nivel","Úroveň","Уровень","等级");
        case"era":return p(l,"Epoka","Era","Era","Éra","Эпоха","时代");
        case"persec":return p(l,"/s","/s","/s","/s","/с","/秒");
        case"combo":return p(l,"COMBO","COMBO","COMBO","COMBO","КОМБО","连击");
        case"clicks":return p(l,"kliknięć","clicks","clics","kliknutí","нажатий","点击");
        case"next_combo":return p(l,"do kolejnego mnożnika","to next multiplier","al siguiente multiplicador","do dalšího násobiče","до следующего множителя","到下一倍率");
        case"tap_anywhere":return p(l,"KLIKNIJ GDZIEKOLWIEK","TAP ANYWHERE","TOCA EN CUALQUIER LUGAR","KLIKNI KDEKOLI","НАЖИМАЙ ГДЕ УГОДНО","点击任意位置");
        case"daily":return p(l,"Nagroda dzienna","Daily reward","Recompensa diaria","Denní odměna","Ежедневная награда","每日奖励");
        case"claim":return p(l,"ODBIERZ","CLAIM","RECOGER","VYZVEDNOUT","ЗАБРАТЬ","领取");
        case"event":return p(l,"AKTYWNY EVENT","ACTIVE EVENT","EVENTO ACTIVO","AKTIVNÍ UDÁLOST","АКТИВНОЕ СОБЫТИЕ","活动事件");
        case"gold_rush":return p(l,"Złota gorączka","Gold Rush","Fiebre del oro","Zlatá horečka","Золотая лихорадка","淘金热");
        case"crystal_rain":return p(l,"Deszcz kryształów","Crystal Rain","Lluvia de cristales","Krystalový déšť","Кристальный дождь","水晶雨");
        case"build_fever":return p(l,"Szał budowy","Build Fever","Fiebre de construcción","Stavební horečka","Строительная лихорадка","建造狂热");
        case"xp_boost":return p(l,"Przyspieszenie XP","XP Boost","Impulso de XP","XP bonus","Ускорение XP","经验加成");
        case"buy":return p(l,"KUP","BUY","COMPRAR","KOUPIT","КУПИТЬ","购买");
        case"owned":return p(l,"POSIADANE","OWNED","COMPRADO","VLASTNĚNO","КУПЛЕНО","已拥有");
        case"locked":return p(l,"ZABLOKOWANE","LOCKED","BLOQUEADO","ZAMČENO","ЗАКРЫТО","未解锁");
        case"income":return p(l,"Dochód","Income","Ingresos","Příjem","Доход","收入");
        case"cost":return p(l,"Koszt","Cost","Coste","Cena","Цена","费用");
        case"lvl":return p(l,"POZ.","LVL","NV.","ÚR.","УР.","级");
        case"dps":return p(l,"Moc","Power","Poder","Síla","Сила","战力");
        case"upgrade":return p(l,"ULEPSZ","UPGRADE","MEJORAR","VYLEPŠIT","УЛУЧШИТЬ","升级");
        case"tech_tree":return p(l,"DRZEWKO TECHNOLOGII","TECH TREE","ÁRBOL TECNOLÓGICO","TECH STROM","ДЕРЕВО ТЕХНОЛОГИЙ","科技树");
        case"boss":return p(l,"BOSS","BOSS","JEFE","BOSS","БОСС","首领");
        case"fight_boss":return p(l,"WALCZ Z BOSSEM","FIGHT BOSS","LUCHAR CONTRA JEFE","BOJ S BOSSEM","БОЙ С БОССОМ","挑战首领");
        case"boss_defeated":return p(l,"BOSS POKONANY!","BOSS DEFEATED!","¡JEFE DERROTADO!","BOSS PORAŽEN!","БОСС ПОБЕЖДЁН!","首领已击败！");
        case"offline":return p(l,"Dochód offline","Offline income","Ingresos offline","Offline příjem","Офлайн доход","离线收入");
        case"offline_rule":return p(l,"Maks. 12h • 20% aktualnego dochodu/s","Max 12h • 20% of current income/s","Máx. 12h • 20% del ingreso/s","Max 12h • 20% aktuálního příjmu/s","Макс. 12ч • 20% текущего дохода/с","最多12小时 • 当前每秒收入的20%");
        case"welcome_back":return p(l,"WITAJ Z POWROTEM!","WELCOME BACK!","¡BIENVENIDO!","VÍTEJ ZPĚT!","С ВОЗВРАЩЕНИЕМ!","欢迎回来！");
        case"away":return p(l,"Nie było Cię","You were away","Estuviste fuera","Byl jsi pryč","Вас не было","离开时间");
        case"prestige":return p(l,"ODRODZENIE","REBIRTH","RENACER","ZROZENÍ","ПЕРЕРОЖДЕНИЕ","重生");
        case"prestige_desc":return p(l,"Zresetuj zwykły postęp i zdobądź stały mnożnik.","Reset normal progress for a permanent multiplier.","Reinicia el progreso por un multiplicador permanente.","Resetuj postup pro trvalý násobič.","Сбросьте прогресс ради постоянного множителя.","重置普通进度并获得永久倍率。");
        case"settings":return p(l,"Ustawienia","Settings","Ajustes","Nastavení","Настройки","设置");
        case"sound":return p(l,"Dźwięki","Sound","Sonido","Zvuk","Звук","声音");
        case"haptics":return p(l,"Wibracje","Haptics","Vibración","Vibrace","Вибрация","振动");
        case"power":return p(l,"Oszczędzanie energii","Low power","Ahorro de energía","Úspora energie","Экономия энергии","省电模式");
        case"language":return p(l,"Język","Language","Idioma","Jazyk","Язык","语言");
        case"stats":return p(l,"Statystyki","Stats","Estadísticas","Statistiky","Статистика","统计");
        case"total_taps":return p(l,"Łącznie kliknięć","Total taps","Clics totales","Kliknutí celkem","Всего нажатий","总点击");
        case"total_earned":return p(l,"Łącznie zarobiono","Total earned","Total ganado","Celkem vyděláno","Всего заработано","总收入");
        case"best_combo":return p(l,"Najlepsze combo","Best combo","Mejor combo","Nejlepší combo","Лучшее комбо","最佳连击");
        case"max_level":return p(l,"Najwyższy poziom","Highest level","Nivel máximo","Nejvyšší úroveň","Макс. уровень","最高等级");
        case"reset":return p(l,"RESET ZAPISU","RESET SAVE","BORRAR PARTIDA","RESET HRY","СБРОС СОХРАНЕНИЯ","重置存档");
        case"complete":return p(l,"UKOŃCZONE","COMPLETE","COMPLETADO","HOTOVO","ВЫПОЛНЕНО","完成");
        case"reward":return p(l,"Nagroda","Reward","Recompensa","Odměna","Награда","奖励");
        case"mission":return p(l,"Misja","Mission","Misión","Mise","Миссия","任务");
        case"achievement":return p(l,"Osiągnięcia","Achievements","Logros","Úspěchy","Достижения","成就");
        case"new_era":return p(l,"NOWA EPOKA!","NEW ERA!","¡NUEVA ERA!","NOVÁ ÉRA!","НОВАЯ ЭПОХА!","新时代！");
        case"critical":return p(l,"KRYTYK!","CRITICAL!","¡CRÍTICO!","KRITICKÝ!","КРИТ!","暴击！");
        case"menu":return p(l,"Menu","Menu","Menú","Menu","Меню","菜单");
        case"skip":return p(l,"POMIŃ","SKIP","SALTAR","PŘESKOČIT","ПРОПУСТИТЬ","跳过");
        default:return k;
    }}

    public static String stage(String l,int stage){
        if("pl".equals(l))return V3Content.stageName(stage);
        if(stage==39)return p(l,"Wieża Nieskończoności","Infinity Tower","Torre Infinita","Věž nekonečna","Башня Бесконечности","无限之塔");
        String group;
        if(stage<5)group=p(l,"Początki","Frontier","Frontera","Počátky","Начало","起源");
        else if(stage<12)group=p(l,"Królestwo","Kingdom","Reino","Království","Королевство","王国");
        else if(stage<20)group=p(l,"Przemysł","Industry","Industria","Průmysl","Промышленность","工业");
        else if(stage<26)group=p(l,"Megamiasto","Megacity","Megaciudad","Megaměsto","Мегаполис","巨型城市");
        else if(stage<35)group=p(l,"Kosmos","Space Age","Era espacial","Kosmická éra","Космическая эра","太空时代");
        else group=p(l,"Transcendencja","Transcendence","Trascendencia","Transcendence","Трансценденция","超越");
        return group+" "+(stage+1);
    }

    public static String building(String l,String id){switch(id){
        case"gatherer":return p(l,"Zbieracze","Gatherers","Recolectores","Sběrači","Собиратели","采集者");
        case"woodcutter":return p(l,"Drwale","Woodcutters","Leñadores","Dřevorubci","Лесорубы","伐木工");
        case"farm":return p(l,"Farmy","Farms","Granjas","Farmy","Фермы","农场");
        case"quarry":return p(l,"Kamieniołomy","Quarries","Canteras","Lomy","Карьеры","采石场");
        case"caravan":return p(l,"Karawany","Caravans","Caravanas","Karavany","Караваны","商队");
        case"market":return p(l,"Rynki","Markets","Mercados","Trhy","Рынки","市场");
        case"forge":return p(l,"Kuźnie","Forges","Forjas","Kovárny","Кузницы","锻造厂");
        case"workshop":return p(l,"Warsztaty","Workshops","Talleres","Dílny","Мастерские","工坊");
        case"factory":return p(l,"Fabryki","Factories","Fábricas","Továrny","Фабрики","工厂");
        case"powerplant":return p(l,"Elektrownie","Power Plants","Centrales","Elektrárny","Электростанции","发电站");
        case"datacenter":return p(l,"Centra danych","Data Centers","Centros de datos","Datacentra","Дата-центры","数据中心");
        case"robotics":return p(l,"Robotyka","Robotics","Robótica","Robotika","Робототехника","机器人");
        case"orbital":return p(l,"Port orbitalny","Orbital Port","Puerto orbital","Orbitální port","Орбитальный порт","轨道港");
        case"quantum":return p(l,"Węzeł kwantowy","Quantum Node","Nodo cuántico","Kvantový uzel","Квантовый узел","量子节点");
        default:return p(l,"Rdzeń osobliwości","Singularity Core","Núcleo singular","Jádro singularity","Ядро сингулярности","奇点核心");
    }}
}
