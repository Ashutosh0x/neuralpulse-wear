package androidx.health.connect.client.records

import androidx.health.connect.client.records.metadata.Metadata

class MedicalRecord private constructor() : Record {
    override val metadata: Metadata = Metadata()

    class Builder {
        fun setFhirVersion(version: String): Builder = this
        fun setFhirResourceCategory(category: Int): Builder = this
        fun setPayload(payload: String): Builder = this
        fun build(): MedicalRecord = MedicalRecord()
    }

    companion object {
        const val CATEGORY_LABORATORY = 1
    }
}
