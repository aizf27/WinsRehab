import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.winsrehab.data.dao.DoctorDao
import com.example.winsrehab.data.dao.PatientDao
import com.example.winsrehab.data.entity.Doctor

@Database(entities = [Doctor::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun doctorDao(): DoctorDao
    abstract fun patientDao(): PatientDao
}
