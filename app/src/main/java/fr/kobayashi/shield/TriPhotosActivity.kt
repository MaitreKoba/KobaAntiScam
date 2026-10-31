/*
 * FICHIER : TriPhotosActivity.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/TriPhotosActivity.kt
 * LIGNES : 445 | RÔLE : Tri des photos par dossier : consulter, mode suppression (glisser à gauche supprimer, à droite garder),
 *          vérification avant suppression, confirmation système Android | DATE : 2026-10-31 | VERSION : 1.15.0
 * ORIGINE : Création | AUTEUR : Maitre Kobayashi | STATUT : STABLE | DÉPENDANCES : MediaStore, RecyclerView, Material
 * LIEN : MainActivity.kt | NOTES : rien n'est supprimé avant « Supprimer » puis l'accord dans la fenêtre d'Android.
 *        Suppression en lot : Android 11+ (MediaStore.createDeleteRequest). Miniatures : ContentResolver.loadThumbnail.
 */
package fr.kobayashi.shield

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class TriPhotosActivity : BaseActivity() {

    data class Photo(val id: Long, val uri: Uri)
    data class Dossier(val id: Long, val nom: String, val photos: List<Photo>)

    private enum class Vue { DOSSIERS, GRILLE, TRI, REVUE }

    private val dp by lazy { resources.displayMetrics.density }
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    private lateinit var titre: TextView
    private lateinit var btnTerminer: MaterialButton
    private lateinit var contenu: FrameLayout

    private var vue = Vue.DOSSIERS
    private var dossiers: List<Dossier> = emptyList()
    private var dossier: Dossier? = null
    private var position = 0
    private val choix = mutableListOf<Pair<Photo, Boolean>>()
    private val aSupprimer = linkedSetOf<Photo>()

    private val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE

    private val demandePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) charger() else message("Sans accès aux photos, rien à trier. Autorise-le dans les réglages Android.")
    }

    private val demandeSuppression = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        if (r.resultCode == RESULT_OK) {
            Toast.makeText(this, "${aSupprimer.size} photo(s) supprimée(s)", Toast.LENGTH_LONG).show()
            aSupprimer.clear()
            choix.clear()
            charger()
        } else {
            Toast.makeText(this, "Suppression annulée, rien n'a été effacé", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val racine = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (12 * dp).toInt())
        }
        val entete = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        entete.addView(ImageButton(this).apply {
            setImageResource(R.drawable.ic_arrow_back)
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = "Retour"
            setOnClickListener { retour() }
        })
        titre = TextView(this).apply {
            textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding((8 * dp).toInt(), 0, 0, 0)
        }
        entete.addView(titre, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        btnTerminer = MaterialButton(this).apply {
            text = "Terminer"
            isAllCaps = false
            visibility = View.GONE
            setOnClickListener { afficherRevue() }
        }
        entete.addView(btnTerminer)
        racine.addView(entete)
        contenu = FrameLayout(this)
        racine.addView(contenu, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            topMargin = (12 * dp).toInt()
        })
        setContentView(racine)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = retour()
        })

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) charger()
        else demandePermission.launch(permission)
    }

    private fun retour() {
        when (vue) {
            Vue.DOSSIERS -> finish()
            Vue.GRILLE -> afficherDossiers()
            Vue.REVUE -> afficherTri()
            Vue.TRI -> if (aSupprimer.isEmpty()) afficherDossiers() else MaterialAlertDialogBuilder(this)
                .setTitle("Abandonner le tri ?")
                .setMessage("${aSupprimer.size} photo(s) marquée(s) ne seront pas supprimées.")
                .setNegativeButton("Continuer le tri", null)
                .setPositiveButton("Abandonner") { _, _ ->
                    aSupprimer.clear()
                    choix.clear()
                    afficherDossiers()
                }
                .show()
        }
    }

    private fun charger() {
        lifecycleScope.launch {
            dossiers = withContext(Dispatchers.IO) { lireDossiers() }
            afficherDossiers()
        }
    }

    private fun lireDossiers(): List<Dossier> {
        val parDossier = linkedMapOf<Long, Pair<String, MutableList<Photo>>>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val iDossier = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val iNom = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (c.moveToNext()) {
                val id = c.getLong(iId)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                parDossier.getOrPut(c.getLong(iDossier)) { (c.getString(iNom) ?: "Sans nom") to mutableListOf() }.second.add(Photo(id, uri))
            }
        }
        return parDossier.map { (id, v) -> Dossier(id, v.first, v.second) }.sortedByDescending { it.photos.size }
    }

    private fun message(texte: String) {
        contenu.removeAllViews()
        contenu.addView(TextView(this).apply { text = texte; textSize = 16f; gravity = Gravity.CENTER })
    }

    // ---- Dossiers ----

    private fun afficherDossiers() {
        vue = Vue.DOSSIERS
        titre.text = "Tri des photos"
        btnTerminer.visibility = View.GONE
        if (dossiers.isEmpty()) return message("Aucune photo trouvée.")
        contenu.removeAllViews()
        contenu.addView(grille(2, dossiers.size) { i, case ->
            val d = dossiers[i]
            case.legende.text = "${d.nom}\n${d.photos.size} photo(s)"
            charger(case.image, d.photos.first(), 360)
            case.root.setOnClickListener { choisirAction(d) }
        })
    }

    private fun choisirAction(d: Dossier) {
        MaterialAlertDialogBuilder(this)
            .setTitle("${d.nom} (${d.photos.size})")
            .setItems(arrayOf("Consulter", "Mode suppression")) { _, i ->
                dossier = d
                if (i == 0) afficherGrille() else {
                    position = 0
                    choix.clear()
                    aSupprimer.clear()
                    afficherTri()
                }
            }
            .show()
    }

    // ---- Consulter ----

    private fun afficherGrille() {
        val d = dossier ?: return
        vue = Vue.GRILLE
        titre.text = d.nom
        btnTerminer.visibility = View.GONE
        contenu.removeAllViews()
        contenu.addView(grille(3, d.photos.size) { i, case ->
            val p = d.photos[i]
            case.legende.visibility = View.GONE
            charger(case.image, p, 240)
            case.root.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(p.uri, "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            }
        })
    }

    // ---- Mode suppression ----

    private fun afficherTri() {
        val d = dossier ?: return
        vue = Vue.TRI
        btnTerminer.visibility = View.VISIBLE
        if (position >= d.photos.size) return afficherRevue()
        titre.text = "${position + 1} / ${d.photos.size} · ${aSupprimer.size} à supprimer"
        contenu.removeAllViews()

        val colonne = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val aide = TextView(this).apply {
            text = "← glisse à gauche : supprimer        glisse à droite : garder →"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.ink3))
        }
        colonne.addView(aide)
        val carte = MaterialCardView(this).apply { radius = 18 * dp; cardElevation = 6 * dp }
        val image = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER; setBackgroundColor(Color.BLACK) }
        val tampon = TextView(this).apply {
            textSize = 30f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (8 * dp).toInt())
            alpha = 0f
        }
        carte.addView(image, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        carte.addView(tampon, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = (24 * dp).toInt()
        })
        colonne.addView(carte, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            topMargin = (8 * dp).toInt()
        })
        val photo = d.photos[position]
        charger(image, photo, 1080)

        var x0 = 0f
        carte.setOnTouchListener { v, e ->
            if (!v.isEnabled) return@setOnTouchListener true
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> x0 = e.rawX
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - x0
                    v.translationX = dx
                    v.rotation = dx / 40f
                    tampon.text = if (dx < 0) "SUPPRIMER" else "GARDER"
                    tampon.setTextColor(ContextCompat.getColor(this, if (dx < 0) R.color.red else R.color.succes))
                    tampon.alpha = (abs(dx) / (v.width / 3f)).coerceAtMost(1f)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dx = e.rawX - x0
                    if (abs(dx) > v.width / 3f) decider(v, photo, supprimer = dx < 0)
                    else {
                        v.animate().translationX(0f).rotation(0f).setDuration(150).start()
                        tampon.alpha = 0f
                    }
                }
            }
            true
        }

        val boutons = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(0, (12 * dp).toInt(), 0, 0) }
        fun bouton(texte: String, couleur: Int, action: () -> Unit) = MaterialButton(this).apply {
            text = texte
            isAllCaps = false
            setBackgroundColor(ContextCompat.getColor(context, couleur))
            setOnClickListener { action() }
        }
        boutons.addView(bouton("Supprimer", R.color.red) { decider(carte, photo, true) },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        boutons.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Annuler"
            isAllCaps = false
            isEnabled = choix.isNotEmpty()
            setOnClickListener { annulerDernier() }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = (8 * dp).toInt(); marginEnd = (8 * dp).toInt()
        })
        boutons.addView(bouton("Garder", R.color.succes) { decider(carte, photo, false) },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        colonne.addView(boutons)
        contenu.addView(colonne)
    }

    private fun decider(carte: View, photo: Photo, supprimer: Boolean) {
        if (!carte.isEnabled) return
        carte.isEnabled = false
        val sens = if (supprimer) -1 else 1
        carte.animate().translationX(sens * carte.width * 1.5f).rotation(sens * 20f).setDuration(180).withEndAction {
            choix.add(photo to supprimer)
            if (supprimer) aSupprimer.add(photo)
            position++
            afficherTri()
        }.start()
    }

    private fun annulerDernier() {
        val (photo, _) = choix.removeLastOrNull() ?: return
        aSupprimer.remove(photo)
        position = (position - 1).coerceAtLeast(0)
        afficherTri()
    }

    // ---- Vérification ----

    private fun afficherRevue() {
        vue = Vue.REVUE
        btnTerminer.visibility = View.GONE
        val marquees = choix.filter { it.second }.map { it.first }
        titre.text = "Vérification"
        contenu.removeAllViews()
        if (marquees.isEmpty()) {
            message("Aucune photo marquée à supprimer.")
            return
        }
        val colonne = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        colonne.addView(TextView(this).apply {
            text = "Touche une photo pour la garder finalement (elle s'éclaircit). Les photos en rouge seront supprimées."
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.ink3))
        })
        val confirmer = MaterialButton(this).apply {
            isAllCaps = false
            setBackgroundColor(ContextCompat.getColor(context, R.color.red))
            setOnClickListener { supprimer() }
        }
        fun majBouton() {
            confirmer.text = "Supprimer ${aSupprimer.size} photo(s)"
            confirmer.isEnabled = aSupprimer.isNotEmpty()
        }
        colonne.addView(grille(3, marquees.size) { i, case ->
            val p = marquees[i]
            case.legende.visibility = View.GONE
            charger(case.image, p, 240)
            fun etat() {
                val marque = p in aSupprimer
                case.image.alpha = if (marque) 1f else 0.35f
                case.root.strokeWidth = if (marque) (3 * dp).toInt() else 0
                case.root.strokeColor = ContextCompat.getColor(this, R.color.red)
            }
            etat()
            case.root.setOnClickListener {
                if (!aSupprimer.remove(p)) aSupprimer.add(p)
                etat()
                majBouton()
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = (8 * dp).toInt() })
        majBouton()
        val bas = LinearLayout(this).apply { setPadding(0, (8 * dp).toInt(), 0, 0) }
        bas.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Reprendre le tri"
            isAllCaps = false
            setOnClickListener { afficherTri() }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = (8 * dp).toInt() })
        bas.addView(confirmer, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        colonne.addView(bas)
        contenu.addView(colonne)
    }

    private fun supprimer() {
        if (aSupprimer.isEmpty()) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Toast.makeText(this, "Suppression en lot disponible à partir d'Android 11", Toast.LENGTH_LONG).show()
            return
        }
        val demande = MediaStore.createDeleteRequest(contentResolver, aSupprimer.map { it.uri })
        demandeSuppression.launch(IntentSenderRequest.Builder(demande.intentSender).build())
    }

    // ---- Grille et miniatures ----

    private class Case(val root: MaterialCardView, val image: ImageView, val legende: TextView) : RecyclerView.ViewHolder(root)

    private fun grille(colonnes: Int, nombre: Int, lier: (Int, Case) -> Unit) = RecyclerView(this).apply {
        layoutManager = GridLayoutManager(context, colonnes)
        adapter = object : RecyclerView.Adapter<Case>() {
            override fun getItemCount() = nombre
            override fun onCreateViewHolder(parent: ViewGroup, type: Int): Case {
                val cote = (resources.displayMetrics.widthPixels - (32 * dp).toInt()) / colonnes - (8 * dp).toInt()
                val carte = MaterialCardView(context).apply {
                    radius = 12 * dp
                    layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        setMargins((4 * dp).toInt(), (4 * dp).toInt(), (4 * dp).toInt(), (4 * dp).toInt())
                    }
                }
                val col = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                val image = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                val legende = TextView(context).apply { textSize = 13f; setPadding((8 * dp).toInt(), (6 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt()) }
                col.addView(image, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cote.coerceAtLeast((80 * dp).toInt())))
                col.addView(legende)
                carte.addView(col)
                return Case(carte, image, legende)
            }
            override fun onBindViewHolder(h: Case, i: Int) {
                h.legende.visibility = View.VISIBLE
                h.image.alpha = 1f
                h.root.strokeWidth = 0
                lier(i, h)
            }
        }
    }

    private fun charger(vue: ImageView, photo: Photo, taille: Int) {
        val cle = "${photo.id}_$taille"
        vue.tag = cle
        cache.get(cle)?.let { vue.setImageBitmap(it); return }
        vue.setImageDrawable(null)
        lifecycleScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                runCatching { contentResolver.loadThumbnail(photo.uri, Size(taille, taille), null) }.getOrNull()
            } ?: return@launch
            cache.put(cle, bmp)
            if (vue.tag == cle) vue.setImageBitmap(bmp)
        }
    }
}
