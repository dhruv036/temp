package io.stempedia.pictoblox.firebase.login

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import com.google.firebase.Timestamp
import java.util.*

class PictoBloxDatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val timeStamp: Timestamp = requireArguments().getParcelable("b_day_timestamp")!!
        val date = timeStamp.toDate()

        val day = DateFormat.format("dd", date).toString().toInt()
        val month = DateFormat.format("MM", date).toString().toInt()
        val year = DateFormat.format("yyyy", date).toString().toInt()

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -16)

        val pickerDialog = DatePickerDialog(requireContext(), this, year, month, day)
        pickerDialog.datePicker.maxDate = calendar.timeInMillis

        return pickerDialog
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        parentFragment?.also { frag ->
            if (frag is AdultDetailFragment) {
                frag.onDateSelected(day, month, year)

            } else if (frag is TeacherDetailFragment) {
                frag.onDateSelected(day, month, year)
            }
        }
    }
}