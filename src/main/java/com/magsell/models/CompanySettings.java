package com.magsell.models;

import java.time.LocalDateTime;

/**
 * Model pentru setările companiei
 */
public class CompanySettings {
    private Integer id;
    private String companyName;
    private String cui;
    private String regCom;
    private String address;
    private String city;
    private String county;
    private String country;
    private String phone;
    private String email;
    private String bankAccount;
    private String bankName;
    private Double capitalSocial;
    private Boolean vatPayer;
    private String logoPath;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public CompanySettings() {
        this.country = "România";
        this.vatPayer = true;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public String getCui() {
        return cui;
    }
    
    public void setCui(String cui) {
        this.cui = cui;
    }
    
    public String getRegCom() {
        return regCom;
    }
    
    public void setRegCom(String regCom) {
        this.regCom = regCom;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getCounty() {
        return county;
    }
    
    public void setCounty(String county) {
        this.county = county;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getBankAccount() {
        return bankAccount;
    }
    
    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }
    
    public String getBankName() {
        return bankName;
    }
    
    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
    
    public Double getCapitalSocial() {
        return capitalSocial;
    }
    
    public void setCapitalSocial(Double capitalSocial) {
        this.capitalSocial = capitalSocial;
    }
    
    public Boolean getVatPayer() {
        return vatPayer;
    }
    
    public void setVatPayer(Boolean vatPayer) {
        this.vatPayer = vatPayer;
    }
    
    public String getLogoPath() {
        return logoPath;
    }
    
    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "CompanySettings{" +
                "id=" + id +
                ", companyName='" + companyName + '\'' +
                ", cui='" + cui + '\'' +
                ", regCom='" + regCom + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                '}';
    }
}
