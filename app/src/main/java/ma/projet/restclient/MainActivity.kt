package ma.projet.restclient

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ma.projet.restclient.adapter.CompteAdapter
import ma.projet.restclient.adapter.CompteAdapter.OnDeleteClickListener
import ma.projet.restclient.adapter.CompteAdapter.OnUpdateClickListener
import ma.projet.restclient.entities.Compte
import ma.projet.restclient.repository.CompteRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar

class MainActivity : AppCompatActivity(), OnDeleteClickListener, OnUpdateClickListener {
    private var recyclerView: RecyclerView? = null
    private var adapter: CompteAdapter? = null
    private var formatGroup: RadioGroup? = null
    private var addbtn: FloatingActionButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        setupFormatSelection()
        setupAddButton()

        loadData("JSON")
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        formatGroup = findViewById(R.id.formatGroup)
        addbtn = findViewById(R.id.fabAdd)
    }

    private fun setupRecyclerView() {
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        adapter = CompteAdapter(this, this)
        recyclerView!!.adapter = adapter
    }

    private fun setupFormatSelection() {
        formatGroup!!.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            val format = if (checkedId == R.id.radioJson) "JSON" else "XML"
            loadData(format)
        }
    }

    private fun setupAddButton() {
        addbtn!!.setOnClickListener { v: View? -> showAddCompteDialog() }
    }

    private fun showAddCompteDialog() {
        val builder = AlertDialog.Builder(this@MainActivity)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_compte, null)

        val etSolde = dialogView.findViewById<EditText>(R.id.etSolde)
        val typeGroup = dialogView.findViewById<RadioGroup>(R.id.typeGroup)

        builder.setView(dialogView)
            .setTitle("Ajouter un compte")
            .setPositiveButton("Ajouter") { dialog: DialogInterface?, which: Int ->
                val solde = etSolde.text.toString()
                val type = if (typeGroup.checkedRadioButtonId == R.id.radioCourant)
                    "COURANT"
                else
                    "EPARGNE"

                val formattedDate = currentDateFormatted
                val compte = Compte(null, solde.toDouble(), type, formattedDate)
                addCompte(compte)
            }
            .setNegativeButton("Annuler", null)

        val dialog = builder.create()
        dialog.show()
    }

    private val currentDateFormatted: String
        get() {
            val calendar = Calendar.getInstance()
            val formatter = SimpleDateFormat("yyyy-MM-dd")
            return formatter.format(calendar.time)
        }

    private fun addCompte(compte: Compte) {
        val compteRepository = CompteRepository("JSON")
        compteRepository.addCompte(compte, object : Callback<Compte?> {
            override fun onResponse(call: Call<Compte?>, response: Response<Compte?>) {
                if (response.isSuccessful) {
                    showToast("Compte ajouté")
                    loadData("JSON")
                }
            }

            override fun onFailure(call: Call<Compte?>, t: Throwable) {
                showToast("Erreur lors de l'ajout")
            }
        })
    }

    private fun loadData(format: String) {
        val compteRepository = CompteRepository(format)
        compteRepository.getAllCompte(object : Callback<List<Compte?>?> {
            override fun onResponse(
                call: Call<List<Compte?>?>,
                response: Response<List<Compte?>?>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val comptes = response.body()
                    runOnUiThread { adapter!!.updateData(comptes) }
                }
            }

            override fun onFailure(call: Call<List<Compte?>?>, t: Throwable) {
                showToast("Erreur: " + t.message)
            }
        })
    }

    override fun onUpdateClick(compte: Compte) {
        showUpdateCompteDialog(compte)
    }

    private fun showUpdateCompteDialog(compte: Compte) {
        val builder = AlertDialog.Builder(this@MainActivity)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_compte, null)

        val etSolde = dialogView.findViewById<EditText>(R.id.etSolde)
        val typeGroup = dialogView.findViewById<RadioGroup>(R.id.typeGroup)
        etSolde.setText(compte.solde.toString())
        if (compte.type.equals("COURANT", ignoreCase = true)) {
            typeGroup.check(R.id.radioCourant)
        } else if (compte.type.equals("EPARGNE", ignoreCase = true)) {
            typeGroup.check(R.id.radioEpargne)
        }

        builder.setView(dialogView)
            .setTitle("Modifier un compte")
            .setPositiveButton("Modifier") { dialog: DialogInterface?, which: Int ->
                val solde = etSolde.text.toString()
                val type = if (typeGroup.checkedRadioButtonId == R.id.radioCourant)
                    "COURANT"
                else
                    "EPARGNE"
                compte.solde = solde.toDouble()
                compte.type = type
                updateCompte(compte)
            }
            .setNegativeButton("Annuler", null)

        val dialog = builder.create()
        dialog.show()
    }

    private fun updateCompte(compte: Compte) {
        val compteRepository = CompteRepository("JSON")
        compteRepository.updateCompte(compte.id, compte, object : Callback<Compte?> {
            override fun onResponse(call: Call<Compte?>, response: Response<Compte?>) {
                if (response.isSuccessful) {
                    showToast("Compte modifié")
                    loadData("JSON")
                }
            }

            override fun onFailure(call: Call<Compte?>, t: Throwable) {
                showToast("Erreur lors de la modification")
            }
        })
    }

    override fun onDeleteClick(compte: Compte) {
        showDeleteConfirmationDialog(compte)
    }

    private fun showDeleteConfirmationDialog(compte: Compte) {
        AlertDialog.Builder(this)
            .setTitle("Confirmation")
            .setMessage("Voulez-vous vraiment supprimer ce compte ?")
            .setPositiveButton(
                "Oui"
            ) { dialog: DialogInterface?, which: Int -> deleteCompte(compte) }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun deleteCompte(compte: Compte) {
        val compteRepository = CompteRepository("JSON")
        compteRepository.deleteCompte(compte.id, object : Callback<Void?> {
            override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
                if (response.isSuccessful) {
                    showToast("Compte supprimé")
                    loadData("JSON")
                }
            }

            override fun onFailure(call: Call<Void?>, t: Throwable) {
                showToast("Erreur lors de la suppression")
            }
        })
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
        }
    }
}