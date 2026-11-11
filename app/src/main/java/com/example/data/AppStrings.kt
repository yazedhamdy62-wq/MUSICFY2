package com.example.data

data class AppStrings(
    val appName: String,
    val navHome: String,
    val navLibrary: String,
    val navSettings: String,
    
    // Home
    val searchPlaceholder: String,
    val scanDeviceAudio: String,
    val quickPlaylists: String,
    val allSongs: String,
    val playAll: String,
    val shuffle: String,
    val noAudioFiles: String,
    val noAudioFilesDesc: String,
    val equalizer: String,
    
    // Library
    val libraryTitle: String,
    val searchPlaylistsPlaceholder: String,
    val newPlaylist: String,
    val playlistsHeader: String,
    val createPlaylistTitle: String,
    val playlistNamePrompt: String,
    val createBtn: String,
    val cancelBtn: String,
    val saveBtn: String,
    val renamePlaylist: String,
    val deletePlaylist: String,
    val deleteConfirmPlaylist: String,
    val likedPlaylistName: String,
    val likedPlaylistSubtitle: String,
    val cantDeleteLiked: String,
    
    // Playlist Detail
    val addSongs: String,
    val play: String,
    val emptyPlaylist: String,
    val back: String,
    val songsCountSuffix: String,
    
    // Settings
    val settingsTitle: String,
    val lastScan: String,
    val scanFiles: String,
    val themeSection: String,
    val themeDark: String,
    val themeLight: String,
    val themeSystem: String,
    val languageSection: String,
    val langArabic: String,
    val langEnglish: String,
    val langFrench: String,
    val langArabicSub: String,
    val langEnglishSub: String,
    val langFrenchSub: String,
    val playbackSection: String,
    val eqTitle: String,
    val eqSubtitle: String,
    val sleepTimerTitle: String,
    val sleepTimerOff: String,
    val sleepTimerActive: String,
    val audioQualityTitle: String,
    val audioQualitySubtitle: String,
    val gaplessTitle: String,
    val storageSection: String,
    val cacheSize: String,
    val clearBtn: String,
    val cacheCleared: String,
    val aboutSection: String,
    val aboutSubtitle: String,
    
    // Fullscreen Player
    val playerSongMode: String,
    val playerLyricsMode: String,
    val writeLyrics: String,
    val searchWebLyrics: String,
    val editLyricsTitle: String,
    val editLyricsHint: String,
    val noLyricsAvailable: String,
    val queueTitle: String,
    val queueCount: String,
    
    // Song Options (3-dots)
    val playNext: String,
    val addToPlaylist: String,
    val removeFromPlaylist: String,
    val changeCover: String,
    val editSongInfo: String,
    val deleteSong: String,
    val chooseFromDevice: String,
    val chooseFromDeviceSub: String,
    val presetStyles: String,
    val selectPlaylistTitle: String,
    val editSongTitlePrompt: String,
    val editArtistPrompt: String,
    val songTitleLabel: String,
    val artistLabel: String,
    val deleteSongConfirm: String,
    val removeFromPlaylistConfirm: String,
    val navFolders: String = "المجلدات",
    val foldersTitle: String = "مجلدات الأغاني",
    val searchFoldersHint: String = "ابحث في المجلدات والمسارات...",
    val noFoldersFound: String = "لا توجد مجلدات بعد",
    val noFoldersFoundDesc: String = "قم بفحص جهازك لاكتشاف مجلدات الأغاني وتطبيقات المحادثة والتنزيلات",
    val playFolder: String = "تشغيل المجلد",
    val shuffleFolder: String = "خلط المجلد",
    val backToFolders: String = "العودة للمجلدات"
) {
    val searchHint: String get() = searchPlaceholder
    val createNewPlaylist: String get() = newPlaylist
    val searchPlaylistsHint: String get() = searchPlaylistsPlaceholder
    val deletePlaylistConfirm: String get() = deleteConfirmPlaylist
    val playlistNameLabel: String get() = playlistNamePrompt
    val noSongsFound: String get() = noAudioFiles
    val noSongsSub: String get() = noAudioFilesDesc
    val scanDevice: String get() = scanDeviceAudio
    val shuffleBtn: String get() = shuffle
    val playBtn: String get() = play
    val emptyPlaylistPrompt: String get() = emptyPlaylist
    val addSongsPrompt: String get() = addSongs
    val removedFromPlaylistSuccess: String
        get() = when (navHome) {
            "Home" -> "Removed from playlist successfully"
            "Accueil" -> "Retiré de la playlist avec succès"
            else -> "تمت إزالة الأغنية من قائمة التشغيل بنجاح"
        }
    val lyricsSavedSuccess: String
        get() = when (appName) {
            "Musicfy" -> when (navHome) {
                "Home" -> "Lyrics saved successfully"
                "Accueil" -> "Paroles enregistrées avec succès"
                else -> "تم حفظ كلمات الأغنية بنجاح"
            }
            else -> "تم حفظ كلمات الأغنية بنجاح"
        }
    val playlistRenamedSuccess: String
        get() = when (navHome) {
            "Home" -> "Playlist renamed successfully"
            "Accueil" -> "Liste de lecture renommée avec succès"
            else -> "تم تعديل اسم القائمة بنجاح"
        }
    val playlistDeletedSuccess: String
        get() = when (navHome) {
            "Home" -> "Playlist deleted successfully"
            "Accueil" -> "Liste de lecture supprimée avec succès"
            else -> "تم حذف القائمة بنجاح"
        }
    val addedToPlaylistSuccess: String
        get() = when (navHome) {
            "Home" -> "Added to playlist: %s"
            "Accueil" -> "Ajouté à la playlist : %s"
            else -> "تمت إضافة الأغنية إلى \"%s\" بنجاح"
        }
    val coverUpdatedSuccess: String
        get() = when (navHome) {
            "Home" -> "Song cover updated successfully"
            "Accueil" -> "Couverture mise à jour avec succès"
            else -> "تم تغيير غلاف الأغنية بنجاح"
        }
    val onlineLyricsBtn: String
        get() = when (navHome) {
            "Home" -> "Search online"
            "Accueil" -> "Chercher en ligne"
            else -> "بحث أونلاين"
        }
    val onlineLyricsTitle: String
        get() = when (navHome) {
            "Home" -> "Select lyrics version"
            "Accueil" -> "Choisir les paroles"
            else -> "اختر نسخة الكلمات"
        }
    val onlineLyricsSearching: String
        get() = when (navHome) {
            "Home" -> "Searching for lyrics..."
            "Accueil" -> "Recherche des paroles..."
            else -> "جاري البحث عن الكلمات..."
        }
    val onlineLyricsNoResults: String
        get() = when (navHome) {
            "Home" -> "No lyrics found for this song.\nTry searching in the browser or type them yourself."
            "Accueil" -> "Aucune parole trouvée pour ce morceau.\nEssayez le navigateur ou saisissez-les."
            else -> "لم يتم العثور على كلمات لهذه الأغنية.\nجرّب البحث في المتصفح أو اكتبها بنفسك."
        }
    val onlineLyricsError: String
        get() = when (navHome) {
            "Home" -> "Couldn't connect to the internet.\nCheck your connection and try again."
            "Accueil" -> "Connexion impossible.\nVérifiez Internet et réessayez."
            else -> "تعذّر الاتصال بالإنترنت.\nتأكد من الاتصال وحاول مرة أخرى."
        }
    val onlineLyricsRetry: String
        get() = when (navHome) {
            "Home" -> "Retry"
            "Accueil" -> "Réessayer"
            else -> "إعادة المحاولة"
        }
    val onlineLyricsSynced: String
        get() = when (navHome) {
            "Home" -> "Synced"
            "Accueil" -> "Synchronisées"
            else -> "متزامنة"
        }
    val onlineLyricsPlain: String
        get() = when (navHome) {
            "Home" -> "Text only"
            "Accueil" -> "Texte seul"
            else -> "نص فقط"
        }
    val onlineLyricsSelect: String
        get() = when (navHome) {
            "Home" -> "Select"
            "Accueil" -> "Choisir"
            else -> "اختيار"
        }
}

object LocalizationManager {
    val Arabic = AppStrings(
        appName = "Musicfy",
        navHome = "الرئيسية",
        navLibrary = "المكتبة",
        navSettings = "الإعدادات",
        
        searchPlaceholder = "ابحث عن مسار، فنان...",
        scanDeviceAudio = "فحص أغاني الجهاز",
        quickPlaylists = "قوائم التشغيل",
        allSongs = "جميع الأغاني",
        playAll = "تشغيل الكل",
        shuffle = "خلط",
        noAudioFiles = "لا توجد ملفات صوتية بعد",
        noAudioFilesDesc = "قم بفحص ذاكرة الهاتف لاكتشاف الأغاني الحقيقية تلقائياً من جهازك",
        equalizer = "المعادل",
        
        libraryTitle = "المكتبة الموسيقية",
        searchPlaylistsPlaceholder = "ابحث في قوائم التشغيل...",
        newPlaylist = "إنشاء قائمة",
        playlistsHeader = "قوائم التشغيل",
        createPlaylistTitle = "إنشاء قائمة تشغيل جديدة",
        playlistNamePrompt = "اختر اسماً لقائمة التشغيل الخاصة بك:",
        createBtn = "إنشاء",
        cancelBtn = "إلغاء",
        saveBtn = "حفظ",
        renamePlaylist = "تعديل اسم القائمة",
        deletePlaylist = "حذف القائمة",
        deleteConfirmPlaylist = "هل أنت متأكد من حذف هذه القائمة؟",
        likedPlaylistName = "الأغاني المفضلة",
        likedPlaylistSubtitle = "قائمتك المفضلة",
        cantDeleteLiked = "لا يمكن حذف قائمة الأغاني المفضلة",
        
        addSongs = "إضافة أغانٍ",
        play = "تشغيل",
        emptyPlaylist = "هذه القائمة فارغة حالياً",
        back = "رجوع",
        songsCountSuffix = "أغنية",
        
        settingsTitle = "الإعدادات",
        lastScan = "آخر فحص",
        scanFiles = "فحص الملفات",
        themeSection = "المظهر والسمة (THEME)",
        themeDark = "الداكن",
        themeLight = "الفاتح",
        themeSystem = "تلقائي",
        languageSection = "اللغة (LANGUAGE)",
        langArabic = "العربية",
        langEnglish = "English",
        langFrench = "Français",
        langArabicSub = "العربية (الافتراضية)",
        langEnglishSub = "الإنجليزية",
        langFrenchSub = "الفرنسية",
        playbackSection = "تشغيل الصوت (PLAYBACK)",
        eqTitle = "المعادل الصوتي (Equalizer)",
        eqSubtitle = "10 نطاقات وتأثيرات الباس (Bass Boost)",
        sleepTimerTitle = "مؤقت النوم (Sleep Timer)",
        sleepTimerOff = "متوقف",
        sleepTimerActive = "يعمل (%d دقيقة)",
        audioQualityTitle = "جودة الصوت (Hi-Res Lossless)",
        audioQualitySubtitle = "معالجة 32-bit / 192kHz نقاء فائق",
        gaplessTitle = "تشغيل متصل بدون فواصل (Gapless)",
        storageSection = "التخزين والبيانات (STORAGE)",
        cacheSize = "الذاكرة المؤقتة",
        clearBtn = "مسح",
        cacheCleared = "تم مسح الذاكرة المؤقتة بنجاح",
        aboutSection = "حول MUSICFY",
        aboutSubtitle = "مشغل الصوت فائق النقاء",
        
        playerSongMode = "الأغنية",
        playerLyricsMode = "الكلمات",
        writeLyrics = "كتابة الكلمات",
        searchWebLyrics = "بحث في المتصفح",
        editLyricsTitle = "كتابة كلمات الأغنية يدوياً",
        editLyricsHint = "الصق أو اكتب كلمات الأغنية هنا...",
        noLyricsAvailable = "لا توجد كلمات مكتوبة لهذه الأغنية بعد\nاستخدم الأزرار بالأعلى لإضافتها أو البحث عنها",
        queueTitle = "قائمة الانتظار",
        queueCount = "مسار",
        
        playNext = "التشغيل التالي",
        addToPlaylist = "إضافة لقائمة تشغيل",
        changeCover = "تغيير الغلاف",
        editSongInfo = "تعديل الاسم",
        deleteSong = "حذف الأغنية",
        chooseFromDevice = "اختيار صورة من المعرض",
        chooseFromDeviceSub = "حدد أي صورة من ذاكرة هاتفك",
        presetStyles = "أنماط وتدرجات جاهزة",
        selectPlaylistTitle = "اختر قائمة التشغيل",
        editSongTitlePrompt = "تعديل بيانات الأغنية",
        editArtistPrompt = "اسم الفنان",
        songTitleLabel = "عنوان الأغنية",
        artistLabel = "الفنان",
        deleteSongConfirm = "هل أنت متأكد من حذف هذه الأغنية من التطبيق؟",
        removeFromPlaylist = "إزالة من قائمة التشغيل",
        removeFromPlaylistConfirm = "هل أنت متأكد من إزالة هذه الأغنية من قائمة التشغيل الحالية؟ (لن يتم حذف الأغنية من هاتفك)"
    )

    val English = AppStrings(
        appName = "Musicfy",
        navHome = "Home",
        navLibrary = "Library",
        navSettings = "Settings",
        
        searchPlaceholder = "Search songs, artists...",
        scanDeviceAudio = "Scan Device Audio",
        quickPlaylists = "Playlists",
        allSongs = "All Songs",
        playAll = "Play All",
        shuffle = "Shuffle",
        noAudioFiles = "No audio files found yet",
        noAudioFilesDesc = "Scan device storage to discover songs automatically",
        equalizer = "Equalizer",
        
        libraryTitle = "Music Library",
        searchPlaylistsPlaceholder = "Search playlists...",
        newPlaylist = "New Playlist",
        playlistsHeader = "Playlists",
        createPlaylistTitle = "Create New Playlist",
        playlistNamePrompt = "Enter a name for your playlist:",
        createBtn = "Create",
        cancelBtn = "Cancel",
        saveBtn = "Save",
        renamePlaylist = "Rename Playlist",
        deletePlaylist = "Delete Playlist",
        deleteConfirmPlaylist = "Are you sure you want to delete this playlist?",
        likedPlaylistName = "Liked Songs",
        likedPlaylistSubtitle = "Your favorite tracks",
        cantDeleteLiked = "Liked Songs cannot be deleted",
        
        addSongs = "Add Songs",
        play = "Play",
        emptyPlaylist = "This playlist is currently empty",
        back = "Back",
        songsCountSuffix = "songs",
        
        settingsTitle = "Settings",
        lastScan = "Last scan",
        scanFiles = "Scan Files",
        themeSection = "Appearance (THEME)",
        themeDark = "Dark",
        themeLight = "Light",
        themeSystem = "System",
        languageSection = "Language (LANGUAGE)",
        langArabic = "العربية",
        langEnglish = "English",
        langFrench = "Français",
        langArabicSub = "Arabic",
        langEnglishSub = "English (Current)",
        langFrenchSub = "French",
        playbackSection = "Playback (PLAYBACK)",
        eqTitle = "Equalizer",
        eqSubtitle = "10 bands & Bass Boost effects",
        sleepTimerTitle = "Sleep Timer",
        sleepTimerOff = "Off",
        sleepTimerActive = "Active (%d mins)",
        audioQualityTitle = "Audio Quality (Hi-Res Lossless)",
        audioQualitySubtitle = "32-bit / 192kHz pure fidelity processing",
        gaplessTitle = "Gapless Playback",
        storageSection = "Storage & Data (STORAGE)",
        cacheSize = "Cache Size",
        clearBtn = "Clear",
        cacheCleared = "Cache cleared successfully",
        aboutSection = "About MUSICFY",
        aboutSubtitle = "Ultra-pure Hi-Fi audio player",
        
        playerSongMode = "Song",
        playerLyricsMode = "Lyrics",
        writeLyrics = "Enter Lyrics",
        searchWebLyrics = "Search Web",
        editLyricsTitle = "Enter Lyrics Manually",
        editLyricsHint = "Paste or type lyrics here...",
        noLyricsAvailable = "No lyrics available for this track yet\nUse buttons above to add or search lyrics",
        queueTitle = "Queue",
        queueCount = "tracks",
        
        playNext = "Play Next",
        addToPlaylist = "Add to Playlist",
        changeCover = "Change Cover",
        editSongInfo = "Edit Info",
        deleteSong = "Delete Song",
        chooseFromDevice = "Choose image from device",
        chooseFromDeviceSub = "Select any photo from your phone",
        presetStyles = "Preset color styles",
        selectPlaylistTitle = "Select Playlist",
        editSongTitlePrompt = "Edit Song Details",
        editArtistPrompt = "Artist Name",
        songTitleLabel = "Song Title",
        artistLabel = "Artist",
        deleteSongConfirm = "Are you sure you want to delete this song?",
        removeFromPlaylist = "Remove from Playlist",
        removeFromPlaylistConfirm = "Are you sure you want to remove this song from this playlist? (Song will not be deleted from your device)",
        navFolders = "Folders",
        foldersTitle = "Audio Folders",
        searchFoldersHint = "Search folders and paths...",
        noFoldersFound = "No folders found yet",
        noFoldersFoundDesc = "Scan device storage to discover music, downloads, and chat audio folders",
        playFolder = "Play Folder",
        shuffleFolder = "Shuffle Folder",
        backToFolders = "Back to Folders"
    )

    val French = AppStrings(
        appName = "Musicfy",
        navHome = "Accueil",
        navLibrary = "Bibliothèque",
        navSettings = "Paramètres",
        
        searchPlaceholder = "Rechercher titres, artistes...",
        scanDeviceAudio = "Scanner les audios",
        quickPlaylists = "Listes de lecture",
        allSongs = "Tous les titres",
        playAll = "Tout lire",
        shuffle = "Aléatoire",
        noAudioFiles = "Aucun fichier audio trouvé",
        noAudioFilesDesc = "Analysez le stockage pour détecter les morceaux automatiquement",
        equalizer = "Égaliseur",
        
        libraryTitle = "Bibliothèque",
        searchPlaylistsPlaceholder = "Rechercher des listes...",
        newPlaylist = "Nouvelle liste",
        playlistsHeader = "Listes de lecture",
        createPlaylistTitle = "Créer une nouvelle liste",
        playlistNamePrompt = "Entrez un nom pour votre liste :",
        createBtn = "Créer",
        cancelBtn = "Annuler",
        saveBtn = "Enregistrer",
        renamePlaylist = "Renommer la liste",
        deletePlaylist = "Supprimer la liste",
        deleteConfirmPlaylist = "Voulez-vous vraiment supprimer cette liste ?",
        likedPlaylistName = "Titres likés",
        likedPlaylistSubtitle = "Vos titres préférés",
        cantDeleteLiked = "Impossible de supprimer les titres likés",
        
        addSongs = "Ajouter des titres",
        play = "Lire",
        emptyPlaylist = "Cette liste est vide",
        back = "Retour",
        songsCountSuffix = "morceaux",
        
        settingsTitle = "Paramètres",
        lastScan = "Dernier scan",
        scanFiles = "Scanner",
        themeSection = "Apparence (THÈME)",
        themeDark = "Sombre",
        themeLight = "Clair",
        themeSystem = "Système",
        languageSection = "Langue (LANGUE)",
        langArabic = "العربية",
        langEnglish = "English",
        langFrench = "Français",
        langArabicSub = "Arabe",
        langEnglishSub = "Anglais",
        langFrenchSub = "Français (Actuel)",
        playbackSection = "Lecture audio (LECTURE)",
        eqTitle = "Égaliseur",
        eqSubtitle = "10 bandes & amplification des basses",
        sleepTimerTitle = "Minuteur de veille",
        sleepTimerOff = "Désactivé",
        sleepTimerActive = "Actif (%d mins)",
        audioQualityTitle = "Qualité audio (Hi-Res)",
        audioQualitySubtitle = "Traitement 32-bit / 192kHz haute fidélité",
        gaplessTitle = "Lecture sans blanc (Gapless)",
        storageSection = "Stockage et données (STORAGE)",
        cacheSize = "Mémoire cache",
        clearBtn = "Effacer",
        cacheCleared = "Cache effacé avec succès",
        aboutSection = "À propos de MUSICFY",
        aboutSubtitle = "Lecteur audio ultra haute fidélité",
        
        playerSongMode = "Morceau",
        playerLyricsMode = "Paroles",
        writeLyrics = "Saisir paroles",
        searchWebLyrics = "Rechercher sur le Web",
        editLyricsTitle = "Écrire les paroles manuellement",
        editLyricsHint = "Collez ou écrivez les paroles ici...",
        noLyricsAvailable = "Aucune parole disponible pour ce morceau\nUtilisez les boutons ci-dessus pour ajouter ou chercher",
        queueTitle = "File d'attente",
        queueCount = "pistes",
        
        playNext = "Lire ensuite",
        addToPlaylist = "Ajouter à une playlist",
        changeCover = "Changer la couverture",
        editSongInfo = "Modifier les infos",
        deleteSong = "Supprimer le titre",
        chooseFromDevice = "Choisir une image de l'appareil",
        chooseFromDeviceSub = "Sélectionnez une photo de votre galerie",
        presetStyles = "Styles prédéfinis",
        selectPlaylistTitle = "Sélectionner la playlist",
        editSongTitlePrompt = "Modifier les détails du morceau",
        editArtistPrompt = "Nom de l'artiste",
        songTitleLabel = "Titre du morceau",
        artistLabel = "Artiste",
        deleteSongConfirm = "Voulez-vous vraiment supprimer ce morceau ?",
        removeFromPlaylist = "Retirer de la playlist",
        removeFromPlaylistConfirm = "Voulez-vous vraiment retirer cette chanson de cette playlist ? (Le fichier restera sur votre appareil)",
        navFolders = "Dossiers",
        foldersTitle = "Dossiers Audio",
        searchFoldersHint = "Rechercher des dossiers...",
        noFoldersFound = "Aucun dossier trouvé",
        noFoldersFoundDesc = "Analysez l'appareil pour afficher vos dossiers audio",
        playFolder = "Lire le dossier",
        shuffleFolder = "Lecture aléatoire",
        backToFolders = "Retour aux dossiers"
    )

    fun getStrings(language: AppLanguage): AppStrings {
        return when (language) {
            AppLanguage.ARABIC -> Arabic
            AppLanguage.ENGLISH -> English
            AppLanguage.FRENCH -> French
        }
    }
}
