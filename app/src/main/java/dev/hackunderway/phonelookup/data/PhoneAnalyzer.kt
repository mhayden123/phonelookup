package dev.hackunderway.phonelookup.data

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper
import com.google.i18n.phonenumbers.PhoneNumberToTimeZonesMapper
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder
import java.util.Locale

/**
 * Offline analysis with Google's libphonenumber. This is the part that always
 * works: no network, no API key, no rate limit.
 */
object PhoneAnalyzer {

    private val util: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }
    private val geocoder: PhoneNumberOfflineGeocoder by lazy { PhoneNumberOfflineGeocoder.getInstance() }
    private val carrierMapper: PhoneNumberToCarrierMapper by lazy { PhoneNumberToCarrierMapper.getInstance() }
    private val timeZoneMapper: PhoneNumberToTimeZonesMapper by lazy { PhoneNumberToTimeZonesMapper.getInstance() }

    fun analyze(input: String, region: String): PhoneInfo {
        val trimmed = input.trim()
        val regionCode = region.trim().uppercase(Locale.ROOT).ifBlank { "US" }

        return try {
            val phone = util.parse(trimmed, regionCode)
            val valid = util.isValidNumber(phone)
            val possible = util.isPossibleNumber(phone)
            val locale = Locale.ENGLISH

            PhoneInfo(
                input = trimmed,
                region = regionCode,
                valid = valid,
                possible = possible,
                e164 = util.format(phone, PhoneNumberUtil.PhoneNumberFormat.E164),
                international = util.format(phone, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL),
                national = util.format(phone, PhoneNumberUtil.PhoneNumberFormat.NATIONAL),
                rfc3966 = util.format(phone, PhoneNumberUtil.PhoneNumberFormat.RFC3966),
                countryCode = phone.countryCode,
                nationalNumber = phone.nationalNumber.toString(),
                regionCode = util.getRegionCodeForNumber(phone),
                location = geocoder.getDescriptionForNumber(phone, locale).ifBlank { null },
                carrier = carrierMapper.getNameForNumber(phone, locale).ifBlank { null },
                lineType = describeType(util.getNumberType(phone)),
                timezones = timeZoneMapper.getTimeZonesForNumber(phone)
                    .filterNot { it == "Etc/Unknown" }
            )
        } catch (e: NumberParseException) {
            PhoneInfo(
                input = trimmed,
                region = regionCode,
                valid = false,
                possible = false,
                error = friendlyParseError(e)
            )
        }
    }

    private fun describeType(type: PhoneNumberUtil.PhoneNumberType): String = when (type) {
        PhoneNumberUtil.PhoneNumberType.MOBILE -> "Mobile"
        PhoneNumberUtil.PhoneNumberType.FIXED_LINE -> "Fixed line"
        PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE -> "Fixed line or mobile"
        PhoneNumberUtil.PhoneNumberType.TOLL_FREE -> "Toll free"
        PhoneNumberUtil.PhoneNumberType.PREMIUM_RATE -> "Premium rate"
        PhoneNumberUtil.PhoneNumberType.SHARED_COST -> "Shared cost"
        PhoneNumberUtil.PhoneNumberType.VOIP -> "VoIP"
        PhoneNumberUtil.PhoneNumberType.PERSONAL_NUMBER -> "Personal number"
        PhoneNumberUtil.PhoneNumberType.PAGER -> "Pager"
        PhoneNumberUtil.PhoneNumberType.UAN -> "Universal access number"
        PhoneNumberUtil.PhoneNumberType.VOICEMAIL -> "Voicemail"
        else -> "Unknown"
    }

    private fun friendlyParseError(e: NumberParseException): String = when (e.errorType) {
        NumberParseException.ErrorType.INVALID_COUNTRY_CODE ->
            "No country code found. Pick the right country, or type the number with a leading +."
        NumberParseException.ErrorType.NOT_A_NUMBER ->
            "That does not look like a phone number."
        NumberParseException.ErrorType.TOO_SHORT_NSN,
        NumberParseException.ErrorType.TOO_SHORT_AFTER_IDD ->
            "Too short for a phone number in this country."
        NumberParseException.ErrorType.TOO_LONG ->
            "Too long for a phone number in this country."
        else -> e.message ?: "Could not parse this number."
    }

    /** Every region libphonenumber knows about, for the country picker. */
    fun supportedRegions(): List<RegionOption> =
        util.supportedRegions
            .map { code ->
                RegionOption(
                    code = code,
                    name = Locale("", code).getDisplayCountry(Locale.ENGLISH).ifBlank { code },
                    countryCode = util.getCountryCodeForRegion(code)
                )
            }
            .sortedBy { it.name }
}

data class RegionOption(val code: String, val name: String, val countryCode: Int) {
    val display: String get() = "$name (+$countryCode)"
}
