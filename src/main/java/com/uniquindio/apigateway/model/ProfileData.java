package com.uniquindio.apigateway.model;

public class ProfileData {
    private String personalUrl;
    private String nickname;
    private boolean isContactInfoPublic;
    private String address;
    private String bio;
    private String organization;
    private String country;
    private String socialLinks;

    // Getters y Setters
    public String getPersonalUrl() { return personalUrl; }
    public void setPersonalUrl(String personalUrl) { this.personalUrl = personalUrl; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public boolean getIsContactInfoPublic() { return isContactInfoPublic; }
    public void setIsContactInfoPublic(boolean isContactInfoPublic) { this.isContactInfoPublic = isContactInfoPublic; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getSocialLinks() { return socialLinks; }
    public void setSocialLinks(String socialLinks) { this.socialLinks = socialLinks; }
}
