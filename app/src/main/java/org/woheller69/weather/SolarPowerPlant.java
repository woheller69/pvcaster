package org.woheller69.weather;

import net.e175.klaus.solarpositioning.AzimuthZenithAngle;
import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.Grena3;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class SolarPowerPlant {
    double latitude;
    double longitude;
    double cellsMaxPower;
    double cellsArea;
    double cellsEfficiency;
    double diffuseEfficiency;
    double inverterPowerLimit;
    double inverterEfficiency;
    double azimuthAngle;
    double tiltAngle;
    private int[] shadingElevation;
    private int[] shadingOpacity;

    public SolarPowerPlant(double latitude, double longitude, double cellsMaxPower, double cellsArea, double cellsEfficiency, double diffuseEfficiency, double inverterPowerLimit, double inverterEfficiency, double azimuthAngle, double tiltAngle, int[] shadingElevation, int[] shadingOpacity ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.cellsMaxPower = cellsMaxPower;
        this.cellsArea = cellsArea;
        this.cellsEfficiency = cellsEfficiency / 100;
        this.diffuseEfficiency = diffuseEfficiency / 100;
        this.inverterPowerLimit = inverterPowerLimit;
        this.inverterEfficiency = inverterEfficiency / 100;
        this.azimuthAngle = azimuthAngle;
        this.tiltAngle = tiltAngle;
        this.shadingElevation = shadingElevation;
        this.shadingOpacity = shadingOpacity;

    }

    public float getPower(double solarPowerNormal, double solarPowerDiffuse, long epochTimeSeconds) {
        Instant i = Instant.ofEpochSecond(epochTimeSeconds); //currentTimeMillis is in GMT
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(i, ZoneId.of("GMT"));

        AzimuthZenithAngle position = Grena3.calculateSolarPosition(
                dateTime,
                latitude,
                longitude,
                DeltaT.estimate(dateTime.toLocalDate())); // delta T (s)

        double solarAzimuth = position.getAzimuth();
        double solarElevation = 90 - position.getZenithAngle();

        Double[] directionSun = {Math.sin(solarAzimuth / 180 * Math.PI) * Math.cos(solarElevation / 180 * Math.PI), Math.cos(solarAzimuth / 180 * Math.PI) * Math.cos(solarElevation / 180 * Math.PI), Math.sin(solarElevation / 180 * Math.PI)};
        Double[] normalPanel = {Math.sin(azimuthAngle / 180 * Math.PI) * Math.cos((90 - tiltAngle) / 180 * Math.PI), Math.cos(azimuthAngle / 180 * Math.PI) * Math.cos((90 - tiltAngle) / 180 * Math.PI), Math.sin((90 - tiltAngle) / 180 * Math.PI)};

        double efficiency = 0;  //calculate scalar product of sunDirection and normalPanel vectors
        if(solarPowerNormal>0) {  //only needed if normal radiation is available
            for (int j = 0; j < directionSun.length; j++) {
                efficiency += directionSun[j] * normalPanel[j];
            }

            efficiency = Math.max(0, efficiency); //scalar product is negative if sun points to back of module. set 0 in this case

            if (efficiency > 0) {
                //Calculate shading  in 10 degree ranges, total 36 ranges
                int shadingIndex = ((((int) Math.round((solarAzimuth + 5) / 10)) - 1) % 36 + 36) % 36;
                if (shadingElevation[shadingIndex] > solarElevation) {
                    efficiency *= (double) (100 - shadingOpacity[shadingIndex])/100;
                }
            }
        }
        double dcPower = (solarPowerNormal * efficiency + solarPowerDiffuse * diffuseEfficiency )* cellsEfficiency * cellsArea;

        double acPower = Math.min(dcPower * inverterEfficiency, inverterPowerLimit);

        return (float) acPower;
    }
}
