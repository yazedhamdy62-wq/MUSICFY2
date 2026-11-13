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
    val shareSong: String
        get() = when (navHome) {
            "Home" -> "Share"
            "Accueil" -> "Partager"
            else -> "مشاركة"
        }
    val onlineLyricsSearchHint: String
        get() = when (navHome) {
            "Home" -> "Song title or artist..."
            "Accueil" -> "Titre ou artiste..."
            else -> "اسم الأغنية أو الفنان..."
        }

    private fun tr(ar: String, en: String, fr: String): String = when (navHome) {
        "Home" -> en
        "Accueil" -> fr
        else -> ar
    }

    fun scanDone(n: Int): String =
        tr("تم فحص الذاكرة: تم العثور على $n أغنية", "Scan complete: found $n songs", "Analyse terminée : $n titres trouvés")

    fun newSongsFound(n: Int): String =
        tr("تم العثور على $n أغانٍ جديدة تلقائياً", "Found $n new songs automatically", "$n nouveaux titres trouvés automatiquement")

    val noAudioFound: String
        get() = tr("لم يتم العثور على ملفات صوتية في مسارات الجهاز", "No audio files found on your device", "Aucun fichier audio trouvé sur l'appareil")

    val deleteConfirmed: String
        get() = tr("تم تأكيد حذف الملف من ذاكرة الجهاز بنجاح", "File deleted from your device", "Fichier supprimé de l'appareil")

    val audioPermissionNeeded: String
        get() = tr("يلزم إذن الوصول لملفات الصوت لقراءة الأغاني من جهازك", "Audio access permission is required to read songs from your device", "L'autorisation d'accès audio est requise pour lire les morceaux de l'appareil")

    fun songsDeleted(n: Int): String =
        if (n == 1) {
            tr("تم حذف الأغنية نهائياً من الجهاز", "Song permanently deleted from your device", "Titre supprimé définitivement de l'appareil")
        } else {
            tr("تم حذف $n أغانٍ نهائياً من الجهاز", "$n songs permanently deleted from your device", "$n titres supprimés définitivement de l'appareil")
        }

    fun songCountText(n: Int): String = when (n) {
        0 -> tr("0 أغانٍ", "0 songs", "0 titre")
        1 -> tr("أغنية واحدة", "1 song", "1 titre")
        else -> tr("$n أغانٍ", "$n songs", "$n titres")
    }

    fun likedSubtitleText(n: Int): String =
        tr("المفضلة • ${songCountText(n)}", "Favorites • ${songCountText(n)}", "Favoris • ${songCountText(n)}")

    fun playlistSubtitleText(n: Int): String =
        tr("قائمة تشغيل • ${songCountText(n)}", "Playlist • ${songCountText(n)}", "Playlist • ${songCountText(n)}")

    val customPlaylistSubtitle: String
        get() = tr("قائمة مخصصة • 0 أغانٍ", "Custom playlist • 0 songs", "Playlist personnalisée • 0 titre")

    fun playlistCreated(name: String): String =
        tr("تم إنشاء \"$name\" وستظهر بالصفحة الرئيسية", "\"$name\" created and will appear on the home page", "« $name » créée, elle apparaîtra sur la page d'accueil")

    fun addedToPlayNext(title: String): String =
        tr("تمت إضافة \"$title\" إلى التشغيل التالي", "\"$title\" added to play next", "« $title » ajouté à la lecture suivante")

    val songInfoUpdated: String
        get() = tr("تم تعديل بيانات الأغنية", "Song info updated", "Informations du titre mises à jour")

    fun sleepTimerSet(mins: Int): String =
        tr("تم ضبط مؤقت النوم على $mins دقيقة", "Sleep timer set to $mins minutes", "Minuteur réglé sur $mins minutes")

    val sleepTimerEndOfTrack: String
        get() = tr("سيتم إيقاف التشغيل عند نهاية الأغنية الحالية", "Playback will stop at the end of the current song", "La lecture s'arrêtera à la fin du titre en cours")

    fun addedSongsToPlaylist(n: Int, title: String): String =
        tr("تمت إضافة $n أغانٍ إلى $title", "$n songs added to $title", "$n titres ajoutés à $title")

    val addSongsToPlaylistTitle: String
        get() = tr("إضافة أغانٍ إلى القائمة", "Add songs to playlist", "Ajouter des titres à la playlist")

    val searchByNameArtist: String
        get() = tr("ابحث بالاسم أو الفنان...", "Search by name or artist...", "Rechercher par titre ou artiste...")

    fun availableCount(n: Int): String =
        tr("المتاح: $n مسار", "Available: $n tracks", "Disponibles : $n titres")

    fun selectedOfTotal(sel: Int, total: Int): String =
        tr("تم تحديد: $sel من $total", "Selected: $sel of $total", "Sélectionnés : $sel sur $total")

    val deselectAllSongs: String
        get() = tr("إلغاء التحديد", "Deselect all", "Tout désélectionner")

    val selectAllSongs: String
        get() = tr("تحديد الكل", "Select all", "Tout sélectionner")

    val allSongsAlreadyAdded: String
        get() = tr("جميع أغانيك مضافة بالفعل إلى هذه القائمة!", "All your songs are already in this playlist!", "Tous vos titres sont déjà dans cette playlist !")

    val noSongsMatchSearch: String
        get() = tr("لا توجد أغانٍ تطابق بحثك", "No songs match your search", "Aucun titre ne correspond à votre recherche")

    val addBtnLabel: String
        get() = tr("إضافة", "Add", "Ajouter")

    fun addWithCount(n: Int): String =
        tr("إضافة ($n)", "Add ($n)", "Ajouter ($n)")

    val eqDialogTitle: String
        get() = tr("المعادل الصوتي", "Equalizer", "Égaliseur")

    val eqDialogSubtitle: String
        get() = tr("المعادل والتضخيم المسبق", "Equalizer & Preamp", "Égaliseur et préampli")

    val eqSaved: String
        get() = tr("تم الحفظ", "Saved", "Enregistré")

    val queueSheetTitle: String
        get() = tr("قائمة الانتظار والتشغيل التالي", "Queue & Play Next", "File d'attente et lecture suivante")

    fun queueWaitingCount(n: Int): String =
        tr("$n أغانٍ في الانتظار (اسحب للترتيب)", "$n songs waiting (drag to reorder)", "$n titres en attente (glisser pour réorganiser)")

    val nowPlayingNow: String
        get() = tr("قيد التشغيل الآن", "Now playing", "En cours de lecture")

    val nextInQueue: String
        get() = tr("التالي في قائمة التشغيل", "Next in queue", "Suivant dans la file")

    val longPressReorder: String
        get() = tr("اضغط مطولاً للسحب وتغيير الترتيب", "Long-press to drag and reorder", "Appuyez longuement pour réorganiser")

    val queueEmptyMsg: String
        get() = tr("لا توجد أغانٍ قادمة في قائمة الانتظار", "No upcoming songs in the queue", "Aucun titre à venir dans la file")

    val chooseQueueAction: String
        get() = tr("اختر الإجراء لترتيب تشغيل هذه الأغنية في قائمة الانتظار:", "Choose how to place this song in the queue:", "Choisissez comment placer ce titre dans la file :")

    val playNextFirst: String
        get() = tr("تشغيل أولاً (التالي مباشرة بالدور)", "Play next (first in line)", "Lire ensuite (en premier)")

    val playNowInstantly: String
        get() = tr("تشغيل الآن فوراً", "Play now", "Lire maintenant")

    val moveToQueueEnd: String
        get() = tr("نقل إلى نهاية قائمة الانتظار", "Move to end of queue", "Déplacer à la fin de la file")

    val removeFromQueueAction: String
        get() = tr("إزالة من قائمة الانتظار", "Remove from queue", "Retirer de la file")

    val sleepTimerTurnOff: String
        get() = tr("إيقاف المؤقت", "Turn off timer", "Désactiver le minuteur")

    fun minutesLabel(n: Int): String =
        tr("$n دقيقة", "$n minutes", "$n minutes")

    val oneHourLabel: String
        get() = tr("ساعة واحدة", "1 hour", "1 heure")

    val sleepTimerDialogTitle: String
        get() = tr("مؤقت النوم", "Sleep timer", "Minuteur de sommeil")

    val sleepTimerRunning: String
        get() = tr("المؤقت يعمل الآن:", "Timer running:", "Minuteur actif :")

    fun sleepTimeRemaining(timeStr: String): String =
        tr("$timeStr متبقية", "$timeStr remaining", "$timeStr restant")

    val endOfCurrentTrack: String
        get() = tr("نهاية المسار الحالي", "End of current track", "Fin du titre en cours")

    val noMatchingResults: String
        get() = tr("لا توجد نتائج مطابقة", "No matching results", "Aucun résultat correspondant")

    val tryOtherFolderSearch: String
        get() = tr("جرب البحث باسم مجلد أو مسار آخر", "Try searching another folder name or path", "Essayez un autre nom de dossier ou chemin")

    val scanStorageAndApps: String
        get() = tr("فحص الذاكرة والتطبيقات", "Scan storage & apps", "Analyser le stockage et les apps")

    fun searchInFolder(name: String): String =
        tr("بحث داخل $name...", "Search in $name...", "Rechercher dans $name...")

    fun durationHoursMinutes(totalSec: Int): String {
        val totalMins = totalSec / 60
        val hours = totalMins / 60
        val mins = totalMins % 60
        return if (hours > 0) {
            tr("$hours س $mins د", "${hours}h ${mins}m", "${hours} h ${mins} min")
        } else {
            tr("$mins دقيقة", "$mins min", "$mins min")
        }
    }

    val localLibraryLabel: String
        get() = tr("المكتبة المحلية", "LOCAL LIBRARY", "BIBLIOTHÈQUE LOCALE")

    fun selectedCountLabel(n: Int): String =
        tr("محدد ($n)", "Selected ($n)", "Sélectionnés ($n)")

    val deselectAllShort: String
        get() = tr("إلغاء الكل", "Deselect all", "Tout désélectionner")

    fun deleteWithCount(n: Int): String =
        tr("حذف ($n)", "Delete ($n)", "Supprimer ($n)")

    fun deleteSongsTitle(n: Int): String =
        tr("حذف $n أغانٍ؟", "Delete $n songs?", "Supprimer $n titres ?")

    val deleteNowConfirmMsg: String
        get() = tr("هل أنت متأكد من رغبتك في حذف الأغاني المحددة نهائياً من التطبيق وقوائم التشغيل؟", "Are you sure you want to permanently delete the selected songs from the app and playlists?", "Voulez-vous vraiment supprimer définitivement les titres sélectionnés de l'application et des playlists ?")

    val deletePermanentlyBtn: String
        get() = tr("حذف نهائي", "Delete permanently", "Supprimer définitivement")

    fun defaultPlaylistName(n: Int): String =
        tr("قائمة تشغيل $n", "Playlist $n", "Playlist $n")

    val newPlaylistFallback: String
        get() = tr("قائمة جديدة", "New playlist", "Nouvelle playlist")

    val refreshScanDesc: String
        get() = tr("تحديث فحص جميع مسارات ومجلدات الهاتف", "Rescan all folders and paths on your phone", "Réanalyser tous les dossiers du téléphone")

    fun autoThemeActive(isDark: Boolean): String {
        val state = if (isDark) {
            tr("داكن حالياً", "currently dark", "actuellement sombre")
        } else {
            tr("فاتح حالياً", "currently light", "actuellement clair")
        }
        return tr(
            "الوضع التلقائي نشط • يتبع سمة هاتفك ($state)",
            "Auto mode active • follows your phone theme ($state)",
            "Mode auto actif • suit le thème du téléphone ($state)"
        )
    }

    val glassEffectTitle: String
        get() = tr("مؤثرات الزجاج السائل", "Liquid glass effect", "Effet verre liquide")

    val glassEffectDesc: String
        get() = tr("التمويه الشفاف وتأثيرات الإضاءة العائمة في الشريط والمشغل", "Translucent blur and floating light effects on the bar and player", "Flou translucide et effets lumineux flottants sur la barre et le lecteur")

    val gaplessDesc: String
        get() = tr("تشغيل الأغاني المتتابعة بدون فترات صمت", "Play consecutive songs without silent gaps", "Lecture enchaînée sans silence entre les titres")

    val optSubQueue: String
        get() = tr("قائمة الانتظار", "Queue", "File")

    val optSubPlaylists: String
        get() = tr("القوائم", "Playlists", "Playlists")

    val optSubArtwork: String
        get() = tr("الغلاف", "Artwork", "Pochette")

    val optSubTags: String
        get() = tr("البيانات", "Tags", "Métadonnées")

    val optSubAudio: String
        get() = tr("ملف صوتي", "Audio file", "Fichier audio")

    val optSubDeviceApp: String
        get() = tr("الجهاز والتطبيق", "Device & app", "Appareil et app")

    val optSubRemove: String
        get() = tr("إزالة", "Remove", "Retirer")

    // ---- Labels that used to be hardcoded in the UI (sort menu, equalizer, covers, folders...) ----
    val sortDateAdded: String get() = tr("تاريخ الإضافة", "Date Added", "Date d'ajout")
    val sortTitle: String get() = tr("العنوان", "Title", "Titre")
    val sortArtist: String get() = tr("الفنان", "Artist", "Artiste")
    val sortDuration: String get() = tr("المدة", "Duration", "Durée")

    /** [key] is the internal sort key used by the Home screen ("Date Added", "Title", ...). */
    fun sortLabel(key: String): String = when (key) {
        "Title" -> sortTitle
        "Artist" -> sortArtist
        "Duration" -> sortDuration
        else -> sortDateAdded
    }

    /** [key] is the internal equalizer preset name. */
    fun eqPresetLabel(key: String): String = when (key) {
        "Bass Boost" -> tr("تعزيز الباس", "Bass Boost", "Basses renforcées")
        "Vocal & Bass" -> tr("صوت وباس", "Vocal & Bass", "Voix et basses")
        "Rock" -> tr("روك", "Rock", "Rock")
        "Electronic" -> tr("إلكتروني", "Electronic", "Électronique")
        "Flat" -> tr("محايد", "Flat", "Neutre")
        else -> key
    }

    fun coverPresetLabel(key: String): String = when (key) {
        "Tawahin Radial" -> tr("طواحين", "Tawahin Radial", "Tawahin radial")
        "Synthwave Pink" -> tr("سينثويف وردي", "Synthwave Pink", "Synthwave rose")
        "Neon Cyber" -> tr("نيون سايبر", "Neon Cyber", "Cyber néon")
        "Sunset Orange" -> tr("برتقالي الغروب", "Sunset Orange", "Orange coucher de soleil")
        "Deep Ocean" -> tr("المحيط العميق", "Deep Ocean", "Océan profond")
        "Vintage Vinyl" -> tr("فينيل كلاسيكي", "Vintage Vinyl", "Vinyle vintage")
        else -> key
    }

    val playlistWord: String get() = tr("قائمة تشغيل", "Playlist", "Liste de lecture")

    /** Folder names are generated in code (see FolderManager); translate them when displayed. */
    fun folderDisplayName(raw: String): String = when {
        raw == "Downloads / التنزيلات" -> tr("التنزيلات", "Downloads", "Téléchargements")
        raw == "Bluetooth / بلوتوث" -> tr("بلوتوث", "Bluetooth", "Bluetooth")
        raw == "Recordings / التسجيلات" -> tr("التسجيلات", "Recordings", "Enregistrements")
        raw == "Music / الموسيقى" -> tr("الموسيقى", "Music", "Musique")
        raw.startsWith("Music / ") ->
            tr("الموسيقى", "Music", "Musique") + " / " + raw.removePrefix("Music / ")
        raw == "مجلد النظام" || raw == "مجلد عام" -> tr("مجلد النظام", "System folder", "Dossier système")
        raw == "WhatsApp Audio" -> tr("صوتيات واتساب", "WhatsApp Audio", "Audio WhatsApp")
        raw == "WhatsApp Voice Notes" -> tr("ملاحظات واتساب الصوتية", "WhatsApp Voice Notes", "Notes vocales WhatsApp")
        raw == "WhatsApp Documents" -> tr("مستندات واتساب", "WhatsApp Documents", "Documents WhatsApp")
        raw == "Telegram Audio" -> tr("صوتيات تيليجرام", "Telegram Audio", "Audio Telegram")
        raw == "SnapTube Downloads" -> tr("تنزيلات SnapTube", "SnapTube Downloads", "Téléchargements SnapTube")
        raw == "VidMate Downloads" -> tr("تنزيلات VidMate", "VidMate Downloads", "Téléchargements VidMate")
        else -> raw
    }

    fun folderBadgeDisplay(raw: String): String = when (raw) {
        "مجلد" -> tr("مجلد", "Folder", "Dossier")
        "Downloads" -> tr("تنزيلات", "Downloads", "Téléchargements")
        "Recordings" -> tr("تسجيلات", "Recordings", "Enregistrements")
        "Music" -> tr("موسيقى", "Music", "Musique")
        else -> raw
    }

    // Notification / widget texts
    val defaultPlayerArtist: String get() = tr("مشغل الموسيقى", "Music player", "Lecteur de musique")
    val notifChannelName: String
        get() = tr("تشغيل الموسيقى - Musicfy", "Music playback - Musicfy", "Lecture musicale - Musicfy")
    val notifChannelDesc: String
        get() = tr(
            "إشعار التحكم في تشغيل الموسيقى وشاشة القفل",
            "Music playback controls and lock screen notification",
            "Contrôles de lecture et notification de l'écran de verrouillage"
        )

    fun displayArtist(raw: String): String {
        val t = raw.trim()
        return if (t == "فنان غير معروف" || t == "<unknown>" || t.equals("unknown", ignoreCase = true) || t.equals("unknown artist", ignoreCase = true)) {
            tr("فنان غير معروف", "Unknown artist", "Artiste inconnu")
        } else {
            raw
        }
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
